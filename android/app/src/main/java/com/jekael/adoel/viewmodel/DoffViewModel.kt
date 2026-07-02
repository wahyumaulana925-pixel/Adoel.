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
        val next = transform(_state.value)
        _state.value = next
        viewModelScope.launch { repo.save(next) }
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
            return ProsesResult.Err("Mc $mcNo dilewati (-)")

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
                val y = ydMatch.groupValues[2].replace(',', '.').toDoubleOrNull()
                if (y != null) { customYard = y; continue }
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

    fun updateAktual(id: Int, ket: String, corakOverride: String?, customYard: Double?) = updateState { s ->
        s.copy(
            aktual = s.aktual.map {
                if (it.id == id) it.copy(ket = ket, corakOverride = corakOverride, customYard = customYard) else it
            },
        )
    }

    fun finishShift() = updateState { s ->
        s.copy(estimasi = emptyMap(), aktual = emptyList())
    }

    fun setThemeMode(mode: String) = updateState { s ->
        s.copy(themeMode = mode)
    }
}
