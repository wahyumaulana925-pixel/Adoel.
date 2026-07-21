package com.jekael.adoel.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jekael.adoel.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Caps how long past shifts are kept in DoffState.history by calendar age (not shift count), so
// the DataStore JSON blob (the whole state is one serialized blob, see DoffRepository) doesn't
// grow unbounded over months of use, while still keeping a full rolling month of history for
// reporting regardless of how many shifts happen to fall inside it.
private const val HISTORY_RETENTION_DAYS = 30
private const val HISTORY_RETENTION_MIN = HISTORY_RETENTION_DAYS * 24 * 60L

class DoffViewModel @JvmOverloads constructor(
    app: Application,
    // Injectable hanya untuk unit test (fake in-memory). @JvmOverloads menghasilkan konstruktor
    // sekunder (Application) sehingga AndroidViewModelFactory bawaan tetap menemukannya via
    // refleksi — tanpa itu, factory default gagal membuat ViewModel ini.
    private val repo: DoffStateStore = DoffRepository.getInstance(app),
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(DoffState(db = buildDefaultDb()))
    val state: StateFlow<DoffState> = _state.asStateFlow()

    // _state starts out holding a placeholder (174 unconfigured machines) until the real
    // persisted data finishes loading from disk — isLoaded lets the UI show a brief loading
    // placeholder instead of that placeholder data if the very first frame renders before
    // observeState()'s first emission arrives.
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeState().collect {
                _state.value = it
                _isLoaded.value = true
            }
        }
    }

    private fun updateState(transform: (DoffState) -> DoffState) {
        // Optimistic in-memory apply for instant UI feedback...
        _state.value = transform(_state.value)
        // ...while the authoritative write re-applies the same transform atomically against the
        // persisted state (DataStore serializes transactions), so a concurrent writer such as the
        // notification action's quickDoff can't clobber it. observeState() then reconciles _state
        // to the merged result.
        viewModelScope.launch { repo.update(debounceWidgetRefresh = true, transform = transform) }
    }

    fun setMesin(mcNo: String, data: MesinData) = updateState { s ->
        s.copy(db = s.db + (mcNo to data))
    }

    fun resetMesin(mcNo: String) = updateState { s ->
        val default = buildDefaultDb()[mcNo] ?: MesinData()
        s.copy(db = s.db + (mcNo to default))
    }

    fun resetDb() = updateState {
        DoffState(db = buildDefaultDb())
    }

    fun prosesBarisKondisiMesin(ln: String, nowAbsMin: Long): ProsesResult {
        val parts = ln.trim().split(Regex("\\s+"))
        if (parts.size < 2) return ProsesResult.Err("Kurang data")
        val mcNo = parts[0]
        if (!mcNo.matches(Regex("^\\d{1,3}$"))) return ProsesResult.Err("Nomor mesin tidak valid")
        val mesin = _state.value.db[mcNo] ?: return ProsesResult.Err("Mc $mcNo tidak ditemukan")
        if (mesin.corak.isBlank() || mesin.corak.trim() == "-")
            return ProsesResult.Err("Mc $mcNo belum diatur, atur corak dulu di Pengaturan")

        val estAbs: Long = when (mesin.tipe) {
            MesinTipe.TAPPET, MesinTipe.CAM -> {
                val sisaMin = parseDurasi(parts[1]) ?: return ProsesResult.Err("Durasi tidak valid")
                nowAbsMin + sisaMin
            }
            MesinTipe.D405 -> {
                val yardStr = parts[1].trimEnd('y', 'Y')
                val yardBerjalan = yardStr.replace(',', '.').toDoubleOrNull()
                    ?: return ProsesResult.Err("Yard tidak valid")
                val existing = _state.value.estimasi[mcNo]
                val target = existing?.yardOverride ?: mesin.targetYard
                    ?: return ProsesResult.Err("Data target kosong")
                val speed = mesin.speed ?: return ProsesResult.Err("Data speed kosong")
                if (speed <= 0.0) return ProsesResult.Err("Speed harus > 0")
                nowAbsMin + sisaMenitD405(target, yardBerjalan, speed)
            }
            MesinTipe.D408 -> {
                val jamMin = parseJam(parts[1]) ?: return ProsesResult.Err("Jam counter tidak valid")
                val koreksi = mesin.koreksi ?: return ProsesResult.Err("Menit koreksi kosong")
                estAbsD408(jamMin, koreksi)
            }
        }

        val existing = _state.value.estimasi[mcNo]
        val newEst = Estimasi(
            mcNo = mcNo,
            estAbsMin = estAbs,
            startAbsMin = existing?.startAbsMin ?: nowAbsMin,
            corakOverride = existing?.corakOverride,
            yardOverride = existing?.yardOverride,
        )
        updateState { s -> s.copy(estimasi = s.estimasi + (mcNo to newEst)) }

        return ProsesResult.Ok(
            msg = "Mc $mcNo → ${absMinToTimeStr(estAbs)}",
            mcNo = mcNo,
            estAbs = estAbs,
        )
    }

    fun prosesBarisUmum(ln: String): ProsesResult {
        val parts = ln.trim().split(Regex("\\s+"))
        if (parts.isEmpty()) return ProsesResult.Err("Kosong")
        val mcNo = parts[0]
        if (!mcNo.matches(Regex("^\\d{1,3}$"))) return ProsesResult.Err("Nomor mesin tidak valid")
        val mesin = _state.value.db[mcNo] ?: return ProsesResult.Err("Mc $mcNo tidak ditemukan")

        val jam = nowTimeStr()
        var customYard: Double? = null
        val ketTokens = mutableListOf<String>()

        for (i in 1 until parts.size) {
            val token = parts[i]
            val ydMatch = Regex("""^(\+?)([\d.,]+)y?$""", RegexOption.IGNORE_CASE).matchEntire(token)
            if (ydMatch != null) {
                val isDelta = ydMatch.groupValues[1] == "+"
                val num = ydMatch.groupValues[2].replace(',', '.').toDoubleOrNull()
                if (num != null) {
                    val standard = _state.value.estimasi[mcNo]?.yardOverride ?: mesin.targetYard
                    customYard = resolveYardToken(isDelta, num, standard)
                    continue
                }
            }
            ketTokens.add(token)
        }

        val extra = standarisasiKeterangan(ketTokens.joinToString(" ").trim())
        val ket = if (extra.isNotEmpty()) "$jam($extra)" else jam

        val prevEst = _state.value.estimasi[mcNo]
        val effectiveCorak = prevEst?.corakOverride ?: mesin.corak

        var entryId = 0
        var createdEntry: AktualEntry? = null
        updateState { s ->
            entryId = s.nextId
            val entry = AktualEntry(
                id = entryId,
                mcNo = mcNo,
                jam = jam,
                ket = ket,
                corakOverride = if (effectiveCorak != mesin.corak) effectiveCorak else null,
                customYard = customYard,
                tsEpochMin = nowAbsMin(),
            )
            createdEntry = entry
            s.copy(
                nextId = entryId + 1,
                estimasi = s.estimasi - mcNo,
                aktual = listOf(entry) + s.aktual,
            )
        }

        return ProsesResult.Ok(
            msg = "Mc $mcNo ✓",
            mcNo = mcNo,
            prevEst = prevEst,
            undoFn = {
                hapusAktualById(entryId)
                if (prevEst != null) restoreEstimasi(prevEst)
            },
            entry = createdEntry,
        )
    }

    fun hapusEstimasi(mcNo: String) = updateState { s ->
        s.copy(estimasi = s.estimasi - mcNo)
    }

    fun restoreEstimasi(est: Estimasi) = updateState { s ->
        s.copy(estimasi = s.estimasi + (est.mcNo to est))
    }

    /** Freezes Mc [mcNo]'s countdown where it stands right now (see Estimasi.pausedAtAbsMin /
     * effectiveRemaining) — a no-op if already paused or if the estimate no longer exists (e.g.
     * doffed/deleted out from under a pending Jeda tap). */
    fun pauseEstimasi(mcNo: String) = updateState { s ->
        val est = s.estimasi[mcNo] ?: return@updateState s
        if (est.pausedAtAbsMin != null) return@updateState s
        s.copy(estimasi = s.estimasi + (mcNo to est.copy(pausedAtAbsMin = nowAbsMin())))
    }

    /** Un-freezes Mc [mcNo], shifting estAbsMin forward by exactly how long it sat paused so the
     * remaining time the operator saw right before Lanjutkan is preserved instead of having
     * silently ticked down (or up, into overdue) the whole time it was paused. */
    fun resumeEstimasi(mcNo: String) = updateState { s ->
        val est = s.estimasi[mcNo] ?: return@updateState s
        val pausedAt = est.pausedAtAbsMin ?: return@updateState s
        val pausedFor = nowAbsMin() - pausedAt
        s.copy(estimasi = s.estimasi + (mcNo to est.copy(estAbsMin = est.estAbsMin + pausedFor, pausedAtAbsMin = null)))
    }

    fun hapusAktualById(id: Int) = updateState { s ->
        s.copy(aktual = s.aktual.filter { it.id != id })
    }

    fun restoreAktual(entry: AktualEntry) = updateState { s ->
        if (s.aktual.any { it.id == entry.id }) s else s.copy(aktual = listOf(entry) + s.aktual)
    }

    fun hapusShift(id: Int) = updateState { s ->
        s.copy(history = s.history.filter { it.id != id })
    }

    fun updateAktual(id: Int, jam: String, ket: String, corakOverride: String?, customYard: Double?) = updateState { s ->
        s.copy(
            aktual = s.aktual.map {
                if (it.id == id) it.copy(jam = jam, ket = ket, corakOverride = corakOverride, customYard = customYard) else it
            },
        )
    }

    /** Archives the current shift's doffs/estimasi into [DoffState.history] before clearing them,
     * so "Selesai Shift" no longer silently discards a shift's data with no way to look back. */
    fun finishShift() = updateState { s ->
        if (s.aktual.isEmpty() && s.estimasi.isEmpty()) return@updateState s
        val now = nowAbsMin()
        val started = (s.aktual.mapNotNull { it.tsEpochMin } + s.estimasi.values.map { it.startAbsMin })
            .minOrNull() ?: now
        val record = ShiftRecord(
            id = s.nextShiftId,
            startedAtEpochMin = started,
            endedAtEpochMin = now,
            aktual = s.aktual,
            estimasiRemaining = s.estimasi,
        )
        val cutoff = now - HISTORY_RETENTION_MIN
        s.copy(
            estimasi = emptyMap(),
            aktual = emptyList(),
            history = (listOf(record) + s.history).filter { it.endedAtEpochMin >= cutoff },
            nextShiftId = s.nextShiftId + 1,
        )
    }

    fun setThemeMode(mode: String) = updateState { s ->
        s.copy(themeMode = mode)
    }

    fun setOnboardingSeen() = updateState { s ->
        s.copy(onboardingSeen = true)
    }

    /** Full-state backup JSON of the current state. */
    fun exportJson(): String = repo.exportJson(_state.value)

    /** Restore from a backup JSON. [onResult] receives the imported state (null if invalid). */
    fun importJson(json: String, onResult: (DoffState?) -> Unit) {
        viewModelScope.launch { onResult(repo.importJson(json)) }
    }
}
