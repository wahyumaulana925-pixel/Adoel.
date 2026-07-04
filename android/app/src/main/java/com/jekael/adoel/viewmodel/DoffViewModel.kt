package com.jekael.adoel.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jekael.adoel.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Caps how many past shifts are kept in DoffState.history, so the DataStore JSON blob (the
// whole state is one serialized blob, see DoffRepository) doesn't grow unbounded over months of use.
private const val MAX_HISTORY_SHIFTS = 30

class DoffViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = DoffRepository(app)

    private val _state = MutableStateFlow(DoffState(db = buildDefaultDb()))
    val state: StateFlow<DoffState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeState().collect { _state.value = it }
        }
    }

    private fun updateState(transform: (DoffState) -> DoffState) {
        // Optimistic in-memory apply for instant UI feedback...
        _state.value = transform(_state.value)
        // ...while the authoritative write re-applies the same transform atomically against the
        // persisted state (DataStore serializes transactions), so a concurrent writer such as the
        // notification action's quickDoff can't clobber it. observeState() then reconciles _state
        // to the merged result.
        viewModelScope.launch { repo.update(transform) }
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
                val sisaMin = ((target - yardBerjalan) / speed).roundToInt()
                nowAbsMin + sisaMin
            }
            MesinTipe.D408 -> {
                var jamCounterStr = parts[1]
                if (jamCounterStr.equals("c", ignoreCase = true) && parts.size >= 3)
                    jamCounterStr = parts[2]
                val jamMin = parseJam(jamCounterStr) ?: return ProsesResult.Err("Jam counter tidak valid")
                val koreksi = mesin.koreksi?.roundToInt() ?: return ProsesResult.Err("Menit koreksi kosong")
                jamKeShiftAbs(jamMin) + koreksi
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

        if (mesin.tipe == MesinTipe.D408 && parts.getOrNull(1)?.equals("c", ignoreCase = true) == true) {
            return prosesBarisKondisiMesin(ln, nowAbsMin())
        }

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
                    customYard = if (isDelta && standard != null) standard + num else num
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
        )
    }

    fun hapusEstimasi(mcNo: String) = updateState { s ->
        s.copy(estimasi = s.estimasi - mcNo)
    }

    fun restoreEstimasi(est: Estimasi) = updateState { s ->
        s.copy(estimasi = s.estimasi + (est.mcNo to est))
    }

    fun hapusAktualById(id: Int) = updateState { s ->
        s.copy(aktual = s.aktual.filter { it.id != id })
    }

    fun hapusShift(id: Int) = updateState { s ->
        s.copy(history = s.history.filter { it.id != id })
    }

    fun updateAktual(id: Int, ket: String, corakOverride: String?, customYard: Double?) = updateState { s ->
        s.copy(
            aktual = s.aktual.map {
                if (it.id == id) it.copy(ket = ket, corakOverride = corakOverride, customYard = customYard) else it
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
        s.copy(
            estimasi = emptyMap(),
            aktual = emptyList(),
            history = (listOf(record) + s.history).take(MAX_HISTORY_SHIFTS),
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
