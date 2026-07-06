package com.jekael.adoel.viewmodel

import android.app.Application
import com.jekael.adoel.data.ProsesResult
import com.jekael.adoel.data.nowAbsMin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Covers the optimistic in-memory state transitions in DoffViewModel — the per-machine-type
 * estimation math in prosesBarisKondisiMesin/prosesBarisUmum, and finishShift's archive step.
 * The persisted DataStore write (repo.update, launched on viewModelScope) never actually runs
 * here since Dispatchers.Main is a StandardTestDispatcher that's never pumped, so these tests
 * exercise updateState()'s synchronous optimistic-apply path only, not the DataStore round-trip.
 */
class DoffViewModelTest {

    private lateinit var viewModel: DoffViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = DoffViewModel(Application())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun tappetEstimasiUsesRemainingMinutes() {
        // Mc 29 is TAPPET/34758 in the default machine DB (see buildDefaultDb).
        val now = 1_000L
        val result = viewModel.prosesBarisKondisiMesin("29 45", now)

        assertTrue(result is ProsesResult.Ok)
        assertEquals(now + 45, (result as ProsesResult.Ok).estAbs)
        assertEquals(now + 45, viewModel.state.value.estimasi["29"]?.estAbsMin)
    }

    @Test
    fun unconfiguredMachineIsRejected() {
        // Mc 1 defaults to corak "-" (unconfigured) in buildDefaultDb.
        val result = viewModel.prosesBarisKondisiMesin("1 45", 1_000L)

        assertTrue(result is ProsesResult.Err)
    }

    @Test
    fun invalidMachineNumberIsRejected() {
        val result = viewModel.prosesBarisKondisiMesin("abc 45", 1_000L)

        assertTrue(result is ProsesResult.Err)
    }

    @Test
    fun d405EstimasiComputedFromRemainingYardOverSpeed() {
        // Mc 61 is D405/60357, targetYard 303.0, speed 0.158 in the default DB.
        val now = 1_000L
        val result = viewModel.prosesBarisKondisiMesin("61 280y", now)

        assertTrue(result is ProsesResult.Ok)
        val expectedSisaMin = ((303.0 - 280.0) / 0.158).roundToInt()
        assertEquals(now + expectedSisaMin, (result as ProsesResult.Ok).estAbs)
    }

    @Test
    fun d408EstimasiIsAccepted() {
        // Mc 79 is D408/60357, koreksi 18.0 — exact estAbs depends on wall-clock via
        // jamKeShiftAbs, so this only asserts the jam-counter command parses successfully.
        val result = viewModel.prosesBarisKondisiMesin("79 12.30", nowAbsMin())

        assertTrue(result is ProsesResult.Ok)
    }

    @Test
    fun prosesBarisUmumRecordsDoffAndClearsEstimasi() {
        viewModel.prosesBarisKondisiMesin("29 45", 1_000L)
        assertNotNull(viewModel.state.value.estimasi["29"])

        val result = viewModel.prosesBarisUmum("29 HB")

        assertTrue(result is ProsesResult.Ok)
        assertNull(viewModel.state.value.estimasi["29"])
        assertEquals(1, viewModel.state.value.aktual.size)
        assertEquals("29", viewModel.state.value.aktual.first().mcNo)
    }

    @Test
    fun finishShiftArchivesThenClearsCurrentShift() {
        viewModel.prosesBarisUmum("29 HB")
        assertEquals(1, viewModel.state.value.aktual.size)

        viewModel.finishShift()

        assertTrue(viewModel.state.value.aktual.isEmpty())
        assertTrue(viewModel.state.value.estimasi.isEmpty())
        assertEquals(1, viewModel.state.value.history.size)
        assertEquals(1, viewModel.state.value.history.first().aktual.size)
    }
}
