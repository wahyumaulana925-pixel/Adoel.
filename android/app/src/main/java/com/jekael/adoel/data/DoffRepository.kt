package com.jekael.adoel.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("adoel_v5")

private val STATE_KEY = stringPreferencesKey("state_v2")

private data class SerialState(
    val db: Map<String, SerialMesin>,
    val estimasi: Map<String, SerialEstimasi>,
    val aktual: List<SerialAktual>,
    val nextId: Int,
    val themeMode: String?,
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
)

class DoffRepository(private val context: Context) {
    private val gson: Gson = GsonBuilder().create()

    private fun parseState(prefs: Preferences): DoffState {
        return try {
            val json = prefs[STATE_KEY] ?: return DoffState(db = buildDefaultDb())
            val serial = gson.fromJson(json, SerialState::class.java) ?: return DoffState(db = buildDefaultDb())
            DoffState(
                db = serial.db.mapValues { (_, v) ->
                    MesinData(
                        tipe = runCatching { MesinTipe.valueOf(v.tipe) }.getOrDefault(MesinTipe.TAPPET),
                        corak = v.corak,
                        targetYard = v.targetYard,
                        speed = v.speed,
                        koreksi = v.koreksi,
                    )
                },
                estimasi = serial.estimasi.mapValues { (_, v) ->
                    Estimasi(v.mcNo, v.estAbsMin, v.startAbsMin, v.corakOverride, v.yardOverride)
                },
                aktual = serial.aktual.map { a ->
                    AktualEntry(a.id, a.mcNo, a.jam, a.ket, a.corakOverride, a.customYard)
                },
                nextId = serial.nextId,
                themeMode = serial.themeMode ?: "SYSTEM",
            )
        } catch (e: Exception) {
            DoffState(db = buildDefaultDb())
        }
    }

    suspend fun load(): DoffState = parseState(context.dataStore.data.first())

    /** Reactive state — reflects any write, including ones from outside this ViewModel/process
     * lifecycle (e.g. the notification action button). */
    fun observeState(): Flow<DoffState> = context.dataStore.data.map(::parseState)

    private fun serialize(state: DoffState): String {
        val serial = SerialState(
            db = state.db.mapValues { (_, v) ->
                SerialMesin(v.tipe.name, v.corak, v.targetYard, v.speed, v.koreksi)
            },
            estimasi = state.estimasi.mapValues { (_, v) ->
                SerialEstimasi(v.mcNo, v.estAbsMin, v.startAbsMin, v.corakOverride, v.yardOverride)
            },
            aktual = state.aktual.map { a ->
                SerialAktual(a.id, a.mcNo, a.jam, a.ket, a.corakOverride, a.customYard)
            },
            nextId = state.nextId,
            themeMode = state.themeMode,
        )
        return gson.toJson(serial)
    }

    /**
     * Atomically read-modify-write the persisted state inside a single DataStore transaction.
     * DataStore serializes transactions, so concurrent callers (the ViewModel and the
     * notification action's [quickDoff]) can never overwrite each other based on a stale snapshot.
     * [transform] must be pure — it is applied to whatever the latest persisted state is, not to
     * the caller's own in-memory copy.
     */
    suspend fun update(transform: (DoffState) -> DoffState): DoffState {
        lateinit var next: DoffState
        context.dataStore.edit { prefs ->
            next = transform(parseState(prefs))
            prefs[STATE_KEY] = serialize(next)
        }
        return next
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
}
