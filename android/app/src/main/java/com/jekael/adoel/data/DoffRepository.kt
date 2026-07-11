package com.jekael.adoel.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jekael.adoel.widget.AdoelWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("adoel_v5")

private val STATE_KEY = stringPreferencesKey("state_v2")

private data class SerialState(
    val db: Map<String, SerialMesin>,
    val estimasi: Map<String, SerialEstimasi>,
    val aktual: List<SerialAktual>,
    val nextId: Int,
    val themeMode: String?,
    val history: List<SerialShiftRecord>?,
    val nextShiftId: Int?,
    val onboardingSeen: Boolean?,
)

private data class SerialMesin(
    val tipe: String,
    val corak: String,
    val targetYard: Double?,
    val speed: Double?,
    val koreksi: Double?,
)

private data class SerialEstimasi(
    val mcNo: String,
    val estAbsMin: Long,
    val startAbsMin: Long,
    val corakOverride: String?,
    val yardOverride: Double?,
)

private data class SerialAktual(
    val id: Int,
    val mcNo: String,
    val jam: String,
    val ket: String,
    val corakOverride: String?,
    val customYard: Double?,
    val tsEpochMin: Long?,
)

private data class SerialShiftRecord(
    val id: Int,
    val startedAtEpochMin: Long,
    val endedAtEpochMin: Long,
    val aktual: List<SerialAktual>,
    val estimasiRemaining: Map<String, SerialEstimasi>,
)

class DoffRepository(private val context: Context) {
    private val gson: Gson = GsonBuilder().create()

    private fun parseState(prefs: Preferences): DoffState =
        prefs[STATE_KEY]?.let { parseJson(it) } ?: DoffState(db = buildDefaultDb(), onboardingSeen = false)

    /** Parse a persisted or backup JSON snapshot into a [DoffState], or null if it is not a valid
     * Adoel backup (malformed JSON, wrong shape, or missing the machine database). */
    fun parseJson(json: String): DoffState? {
        return try {
            val serial = gson.fromJson(json, SerialState::class.java) ?: return null
            if (serial.db.isEmpty()) return null
            DoffState(
                db = serial.db.mapValues { (mcNo, v) ->
                    MesinData(
                        tipe = runCatching { MesinTipe.valueOf(v.tipe) }.getOrElse {
                            // Silently coercing would hide data corruption AND silently switch the
                            // machine to TAPPET's estimation formula — at least leave a trace.
                            Log.w("DoffRepository", "Tipe mesin tak dikenal '${v.tipe}' di Mc $mcNo, fallback TAPPET")
                            MesinTipe.TAPPET
                        },
                        corak = v.corak,
                        targetYard = v.targetYard,
                        speed = v.speed,
                        koreksi = v.koreksi,
                    )
                },
                estimasi = serial.estimasi.mapValues { (_, v) ->
                    Estimasi(v.mcNo, v.estAbsMin, v.startAbsMin, v.corakOverride, v.yardOverride)
                },
                aktual = dedupeIds(serial.aktual),
                nextId = maxOf(serial.nextId, (serial.aktual.maxOfOrNull { it.id } ?: 0) + 1),
                themeMode = serial.themeMode ?: "SYSTEM",
                history = (serial.history ?: emptyList()).map { r ->
                    ShiftRecord(
                        id = r.id,
                        startedAtEpochMin = r.startedAtEpochMin,
                        endedAtEpochMin = r.endedAtEpochMin,
                        aktual = r.aktual.map { toAktualEntry(it) },
                        estimasiRemaining = r.estimasiRemaining.mapValues { (_, v) ->
                            Estimasi(v.mcNo, v.estAbsMin, v.startAbsMin, v.corakOverride, v.yardOverride)
                        },
                    )
                },
                nextShiftId = serial.nextShiftId ?: 1,
                onboardingSeen = serial.onboardingSeen ?: true,
            )
        } catch (e: Exception) {
            // Null is the correct contract for the caller (invalid backup / corrupt blob), but a
            // systematic serialization break in the field is undiagnosable without this trace.
            Log.w("DoffRepository", "parseJson gagal — bukan snapshot Adoel yang valid", e)
            null
        }
    }

    private fun toAktualEntry(a: SerialAktual): AktualEntry =
        AktualEntry(a.id, a.mcNo, a.jam, a.ket, a.corakOverride, a.customYard, a.tsEpochMin)

    /** Guarantees unique entry ids. Data written before writes became atomic could contain
     * duplicate ids from a race; LazyColumn crashes on duplicate keys, so reassign collisions. */
    private fun dedupeIds(raw: List<SerialAktual>): List<AktualEntry> {
        var nextFree = (raw.maxOfOrNull { it.id } ?: 0) + 1
        val used = HashSet<Int>()
        return raw.map { a ->
            var id = a.id
            if (!used.add(id)) {
                id = nextFree++
                used.add(id)
            }
            toAktualEntry(a.copy(id = id))
        }
    }

    // DataStore reads can surface transient IOExceptions; recover to a default snapshot instead of
    // letting the exception cancel the collector (which would silently freeze all state updates).
    // Logged loudly because downstream this emission is indistinguishable from a fresh install —
    // without the trace, "all my data vanished" reports would have nothing to correlate against.
    private fun DataStore<Preferences>.safeData(): Flow<Preferences> =
        data.catch { e ->
            if (e is IOException) {
                Log.e("DoffRepository", "Gagal baca DataStore — emit state default sementara", e)
                emit(emptyPreferences())
            } else {
                throw e
            }
        }

    suspend fun load(): DoffState = parseState(context.dataStore.safeData().first())

    /** Reactive state — reflects any write, including ones from outside this ViewModel/process
     * lifecycle (e.g. the notification action button). */
    fun observeState(): Flow<DoffState> = context.dataStore.safeData().map(::parseState)

    private fun toSerialAktual(a: AktualEntry): SerialAktual =
        SerialAktual(a.id, a.mcNo, a.jam, a.ket, a.corakOverride, a.customYard, a.tsEpochMin)

    private fun serialize(state: DoffState): String {
        val serial = SerialState(
            db = state.db.mapValues { (_, v) ->
                SerialMesin(v.tipe.name, v.corak, v.targetYard, v.speed, v.koreksi)
            },
            estimasi = state.estimasi.mapValues { (_, v) ->
                SerialEstimasi(v.mcNo, v.estAbsMin, v.startAbsMin, v.corakOverride, v.yardOverride)
            },
            aktual = state.aktual.map(::toSerialAktual),
            nextId = state.nextId,
            themeMode = state.themeMode,
            history = state.history.map { r ->
                SerialShiftRecord(
                    id = r.id,
                    startedAtEpochMin = r.startedAtEpochMin,
                    endedAtEpochMin = r.endedAtEpochMin,
                    aktual = r.aktual.map(::toSerialAktual),
                    estimasiRemaining = r.estimasiRemaining.mapValues { (_, v) ->
                        SerialEstimasi(v.mcNo, v.estAbsMin, v.startAbsMin, v.corakOverride, v.yardOverride)
                    },
                )
            },
            nextShiftId = state.nextShiftId,
            onboardingSeen = state.onboardingSeen,
        )
        return gson.toJson(serial)
    }

    // Lives as long as this DoffRepository instance (the ViewModel's, for the app's foreground
    // lifetime) rather than any single caller's coroutine, so a debounced refresh isn't cancelled
    // just because the mutation that scheduled it already returned.
    private val widgetRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var widgetRefreshJob: Job? = null

    /**
     * Atomically read-modify-write the persisted state inside a single DataStore transaction.
     * DataStore serializes transactions, so concurrent callers (the ViewModel and the
     * notification action's [quickDoff]) can never overwrite each other based on a stale snapshot.
     * [transform] must be pure — it is applied to whatever the latest persisted state is, not to
     * the caller's own in-memory copy.
     *
     * [debounceWidgetRefresh] coalesces rapid back-to-back mutations (e.g. an operator recording
     * several doffs in quick succession while catching up at the end of a shift) into a single
     * widget re-render shortly after the last one, instead of one full Glance recomposition per
     * mutation. Left off (the default) for [quickDoff]'s notification-action path — that runs
     * inside a BroadcastReceiver's goAsync() window, which finishes right after this call returns,
     * so a delayed refresh scheduled there could be killed before it ever runs.
     */
    suspend fun update(debounceWidgetRefresh: Boolean = false, transform: (DoffState) -> DoffState): DoffState {
        lateinit var next: DoffState
        context.dataStore.edit { prefs ->
            next = transform(parseState(prefs))
            prefs[STATE_KEY] = serialize(next)
        }
        // Centralized here (the one funnel every mutator passes through) instead of at each call
        // site, so no future mutator can forget to refresh the widget — and any failure is at
        // least visible in logcat instead of leaving the widget silently stale.
        if (debounceWidgetRefresh) {
            widgetRefreshJob?.cancel()
            widgetRefreshJob = widgetRefreshScope.launch {
                delay(400)
                refreshWidget()
            }
        } else {
            refreshWidget()
        }
        return next
    }

    private suspend fun refreshWidget() {
        try {
            AdoelWidget().updateAll(context)
        } catch (e: Exception) {
            Log.e("DoffRepository", "widget refresh failed", e)
        }
    }

    /** Records a plain doff (no keterangan/yard) for [mcNo] — used by the notification action
     * button. Runs as one atomic transaction so it can't clobber a concurrent in-app write. */
    suspend fun quickDoff(mcNo: String): Boolean {
        var recorded = false
        update { state ->
            val mesin = state.db[mcNo] ?: return@update state
            val prevEst = state.estimasi[mcNo]
            val jam = nowTimeStr()
            val effectiveCorak = prevEst?.corakOverride ?: mesin.corak
            val entry = AktualEntry(
                id = state.nextId,
                mcNo = mcNo,
                jam = jam,
                ket = jam,
                corakOverride = if (effectiveCorak != mesin.corak) effectiveCorak else null,
                customYard = null,
                tsEpochMin = nowAbsMin(),
            )
            recorded = true
            state.copy(
                nextId = state.nextId + 1,
                estimasi = state.estimasi - mcNo,
                aktual = listOf(entry) + state.aktual,
            )
        }
        return recorded
    }

    /** Full-state backup as a JSON string (machine db + estimasi + doff history + theme). */
    fun exportJson(state: DoffState): String = serialize(state)

    /** Restore state from a backup produced by [exportJson]. Returns the imported state, or null
     * if the JSON is not a valid Adoel backup. Writes atomically like any other mutation. */
    suspend fun importJson(json: String): DoffState? {
        val parsed = parseJson(json) ?: return null
        update { parsed }
        return parsed
    }
}
