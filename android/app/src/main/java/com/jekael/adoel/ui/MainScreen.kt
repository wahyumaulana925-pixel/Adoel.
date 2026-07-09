package com.jekael.adoel.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.FreeBreakfast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jekael.adoel.data.*
import com.jekael.adoel.notification.NotificationHelper
import com.jekael.adoel.ui.components.*
import com.jekael.adoel.ui.theme.*
import com.jekael.adoel.viewmodel.DoffViewModel
import com.jekael.adoel.viewmodel.UIViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

internal enum class Mode { ESTIMASI, AKTUAL }

/** Gaps at or above this many minutes between two upcoming doffs are worth flagging as a
 * break window — short enough to still be actionable, long enough to actually leave the floor. */
private const val BREAK_GAP_THRESHOLD_MIN = 30L

internal sealed class MenungguRow {
    data class CardRow(val est: Estimasi) : MenungguRow()
    data class GapRow(val afterMcNo: String, val nextMcNo: String, val gapMin: Long, val nextAbsMin: Long) : MenungguRow()
}

@Composable
fun MainScreen(
    doffVm: DoffViewModel,
    uiVm: UIViewModel,
) {
    // DoffViewModel's _state starts out holding a 174-unconfigured-machine placeholder until the
    // real persisted data finishes loading from disk — bail out to a brief loading screen instead
    // of letting that placeholder data render for a frame or two on a slow cold start.
    val isLoaded by doffVm.isLoaded.collectAsStateWithLifecycle()
    if (!isLoaded) {
        LoadingPlaceholder()
        return
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colors = LocalAppColors.current
    val state by doffVm.state.collectAsStateWithLifecycle()
    val toast by uiVm.toast.collectAsStateWithLifecycle()
    val confirm by uiVm.confirm.collectAsStateWithLifecycle()

    // Console command bar — the one and only way to record estimasi/doffing
    var mode by remember { mutableStateOf(Mode.AKTUAL) }
    var input by remember { mutableStateOf("") }
    var radarFilter by remember { mutableStateOf("") }
    var doffFilter by remember { mutableStateOf("") }

    // Send button feedback — bounce + brief checkmark flash on a successful submit
    var sendPulseKey by remember { mutableStateOf(0) }
    val sendScale = remember { Animatable(1f) }
    var sendShowCheck by remember { mutableStateOf(false) }
    LaunchedEffect(sendPulseKey) {
        if (sendPulseKey == 0) return@LaunchedEffect
        sendShowCheck = true
        sendScale.snapTo(0.8f)
        launch {
            sendScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
        delay(500)
        sendShowCheck = false
    }

    // Error feedback on a rejected console command — a toast alone (3.5s, auto-dismiss) can be
    // missed if the operator looks back at the machine right after hitting send, so this pairs it
    // with a haptic + a brief red ring around the input that doesn't depend on eyes staying on screen.
    var errorFlashKey by remember { mutableStateOf(0) }
    var inputErrorFlash by remember { mutableStateOf(false) }
    LaunchedEffect(errorFlashKey) {
        if (errorFlashKey == 0) return@LaunchedEffect
        inputErrorFlash = true
        delay(200)
        inputErrorFlash = false
    }

    // "Nanti" on the stale-shift banner only snoozes it for this composition/app session — it's
    // not persisted, so if the shift genuinely never gets closed out, the reminder is back next
    // time the app is opened fresh.
    var staleShiftDismissed by remember { mutableStateOf(false) }

    // "Selesai Shift" closes out a full work shift — worth a beat more than a toast that's gone
    // in 3.5s, so this pops a big checkmark over a dimmed backdrop before fading on its own.
    var shiftFinishedKey by remember { mutableStateOf(0) }
    var shiftFinishedVisible by remember { mutableStateOf(false) }
    val shiftCheckScale = remember { Animatable(0f) }
    val shiftBackdropAlpha = remember { Animatable(0f) }
    LaunchedEffect(shiftFinishedKey) {
        if (shiftFinishedKey == 0) return@LaunchedEffect
        shiftFinishedVisible = true
        shiftBackdropAlpha.snapTo(0f)
        shiftCheckScale.snapTo(0f)
        launch { shiftBackdropAlpha.animateTo(1f, tween(150)) }
        shiftCheckScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        delay(700)
        shiftBackdropAlpha.animateTo(0f, tween(250))
        shiftFinishedVisible = false
    }

    var nowAbs by remember { mutableLongStateOf(nowAbsMin()) }
    var notifGranted by remember { mutableStateOf(true) }
    var exactAlarmGranted by remember { mutableStateOf(true) }
    var batteryUnrestricted by remember { mutableStateOf(true) }

    var settingsOpen by remember { mutableStateOf(false) }
    var statistikOpen by remember { mutableStateOf(false) }
    var editAktId by remember { mutableStateOf<Int?>(null) }
    var quickEditMcNo by remember { mutableStateOf<String?>(null) }
    var showRemaining by remember { mutableStateOf(false) }

    val inputFocus = remember { FocusRequester() }
    var consoleBarHeight by remember { mutableStateOf(0.dp) }
    var headerHeight by remember { mutableStateOf(0.dp) }

    // Request notification permission launcher
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
        if (!granted) uiVm.showToast("⚠ Izin notifikasi ditolak")
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= 33) {
                    val nm = context.getSystemService(android.app.NotificationManager::class.java)
                    notifGranted = nm.areNotificationsEnabled()
                }
                if (Build.VERSION.SDK_INT >= 31) {
                    val am = context.getSystemService(AlarmManager::class.java)
                    exactAlarmGranted = am.canScheduleExactAlarms()
                }
                val powerManager = context.getSystemService(PowerManager::class.java)
                batteryUnrestricted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            nowAbs = nowAbsMin()
        }
    }

    val radarList = remember(state.estimasi) {
        sortedByNearest(state.estimasi)
    }
    // Filters by mc number or corak so an operator can jump straight to a machine instead of
    // scanning past everything else when a lot of machines are running at once.
    val filteredRadarList = remember(radarList, radarFilter, state.db) {
        if (radarFilter.isBlank()) {
            radarList
        } else {
            radarList.filter { est ->
                val corak = est.corakOverride ?: state.db[est.mcNo]?.corak ?: ""
                est.mcNo.contains(radarFilter, ignoreCase = true) || corak.contains(radarFilter, ignoreCase = true)
            }
        }
    }
    val (segeraList, menungguList) = remember(filteredRadarList, nowAbs) {
        partitionSegeraMenunggu(filteredRadarList, nowAbs)
    }
    // Real-time reminder of the expected command syntax for the machine number being typed —
    // TAPPET/CAM want a duration, D405 wants running yard, D408 wants a jam counter — so an
    // operator who forgets the format gets a hint before submitting instead of a generic error.
    val inputHint = remember(input, mode, state.db) {
        if (mode != Mode.ESTIMASI) return@remember null
        val mcNo = input.trim().substringBefore(' ')
        if (mcNo.isEmpty() || !mcNo.matches(Regex("^\\d{1,3}$"))) return@remember null
        when (state.db[mcNo]?.tipe) {
            MesinTipe.TAPPET, MesinTipe.CAM -> "Mc $mcNo → sisa menit, cth: $mcNo 45"
            MesinTipe.D405 -> "Mc $mcNo → yard berjalan, cth: $mcNo 280"
            MesinTipe.D408 -> "Mc $mcNo → jam counter, cth: $mcNo 12.30"
            null -> null
        }
    }
    // Flags doff entries left over from a shift the operator forgot to close via "Selesai Shift"
    // before the next 06.00/14.00/22.00 boundary — otherwise they'd silently get archived together
    // with the new shift's entries the next time Selesai Shift is pressed.
    val staleDoffCount = remember(state.aktual, nowAbs) {
        val shiftStart = currentShiftStartAbsMin(nowAbs)
        state.aktual.count { val ts = it.tsEpochMin; ts != null && ts < shiftStart }
    }
    // Un-snooze once the stale batch is actually resolved (e.g. via Selesai Shift), so a *later*
    // forgotten shift isn't silently suppressed by a "Nanti" tap from a previous, unrelated one.
    LaunchedEffect(staleDoffCount == 0) {
        if (staleDoffCount == 0) staleShiftDismissed = false
    }
    // Menunggu bucket spans CALM through IMMINENT (Segera already claims OVERDUE) — tint the
    // band header by its most urgent member so it doesn't read "calm" while cards inside are
    // already amber/orange.
    val menungguAccent = remember(menungguList, nowAbs) {
        when (menungguList.maxOfOrNull { urgencyLevel(it.estAbsMin - nowAbs) }) {
            UrgencyLevel.IMMINENT -> Orange400
            UrgencyLevel.SOON -> Amber400
            else -> Cyan400
        }
    }
    // Flag long idle stretches between two upcoming doffs so the operator knows when it's
    // actually safe to step away, instead of having to eyeball the gap between two times.
    val menungguRows = remember(menungguList) {
        buildList {
            menungguList.forEachIndexed { index, est ->
                add(MenungguRow.CardRow(est))
                val next = menungguList.getOrNull(index + 1)
                if (next != null) {
                    val gap = next.estAbsMin - est.estAbsMin
                    if (gap >= BREAK_GAP_THRESHOLD_MIN) {
                        add(MenungguRow.GapRow(afterMcNo = est.mcNo, nextMcNo = next.mcNo, gapMin = gap, nextAbsMin = next.estAbsMin))
                    }
                }
            }
        }
    }
    fun flashError(msg: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        errorFlashKey++
        uiVm.showToast("⚠ $msg")
    }

    fun submitEstimasi(cmd: String) {
        val result = doffVm.prosesBarisKondisiMesin(cmd, nowAbsMin())
        when (result) {
            is ProsesResult.Ok -> {
                sendPulseKey++
                uiVm.showToast(result.msg)
                input = ""
                result.estAbs?.let { NotificationHelper.scheduleNotif(context, result.mcNo, it) }
            }
            is ProsesResult.Err -> flashError(result.msg)
        }
    }

    fun handleCommand() {
        val cmd = input.trim().uppercase()
        if (cmd.isEmpty()) return

        when (mode) {
            Mode.ESTIMASI -> {
                val mcNo = cmd.substringBefore(' ')
                val existing = state.estimasi[mcNo]
                val remaining = existing?.let { it.estAbsMin - nowAbsMin() }
                // Salah masuk mode ESTIMASI lalu ngetik cepat bisa nggak sadar menimpa timer yang
                // masih jalan — prosesBarisKondisiMesin tidak punya undo, jadi khusus estimasi yang
                // masih aktif & mepet (<10 menit lagi), minta konfirmasi dulu, bukan menimpa diam-diam.
                if (existing != null && remaining != null && remaining in 0 until 10) {
                    uiVm.showConfirm("Mc $mcNo sudah diestimasi ${formatDeltaMin(remaining)} lagi. Timpa dengan estimasi baru?") {
                        submitEstimasi(cmd)
                    }
                } else {
                    submitEstimasi(cmd)
                }
            }
            Mode.AKTUAL -> {
                val result = doffVm.prosesBarisUmum(cmd)
                when (result) {
                    is ProsesResult.Ok -> {
                        sendPulseKey++
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        NotificationHelper.cancelNotif(context, result.mcNo)
                        uiVm.showToast(result.msg, undo = {
                            result.undoFn?.invoke()
                            result.prevEst?.let { NotificationHelper.scheduleNotif(context, it.mcNo, it.estAbsMin) }
                        })
                        input = ""
                    }
                    is ProsesResult.Err -> flashError(result.msg)
                }
            }
        }
    }

    fun handleDoff(mcNo: String) {
        val result = doffVm.prosesBarisUmum(mcNo)
        when (result) {
            is ProsesResult.Ok -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                NotificationHelper.cancelNotif(context, result.mcNo)
                uiVm.showToast(result.msg, undo = {
                    result.undoFn?.invoke()
                    result.prevEst?.let { NotificationHelper.scheduleNotif(context, it.mcNo, it.estAbsMin) }
                })
            }
            is ProsesResult.Err -> uiVm.showToast("⚠ ${result.msg}")
        }
    }

    fun handleHapusEst(mcNo: String) {
        uiVm.showConfirm("Hapus estimasi Mc $mcNo?") {
            val prevEst = state.estimasi[mcNo]
            doffVm.hapusEstimasi(mcNo)
            NotificationHelper.cancelNotif(context, mcNo)
            uiVm.showToast("Mc $mcNo dihapus", undo = {
                if (prevEst != null) {
                    doffVm.restoreEstimasi(prevEst)
                    NotificationHelper.scheduleNotif(context, prevEst.mcNo, prevEst.estAbsMin)
                }
            })
        }
    }

    fun handleHapusAktual(id: Int) {
        val entry = state.aktual.find { it.id == id } ?: return
        uiVm.showConfirm("Hapus riwayat Mc ${entry.mcNo}?") {
            doffVm.hapusAktualById(id)
            editAktId = null
            uiVm.showToast("Mc ${entry.mcNo} dihapus", undo = {
                doffVm.restoreAktual(entry)
            })
        }
    }

    fun handleFinishShift() {
        // finishShift() itself already no-ops on an empty console — mirror that here so an
        // accidental/duplicate tap doesn't show a confirm dialog and checkmark celebration for
        // archiving "0 doff & 0 estimasi".
        if (state.aktual.isEmpty() && state.estimasi.isEmpty()) {
            uiVm.showToast("Tidak ada yang perlu diarsipkan")
            return
        }
        uiVm.showConfirm("Akhiri shift? ${state.aktual.size} doff & ${state.estimasi.size} estimasi akan diarsipkan ke Riwayat, lalu konsol dikosongkan untuk shift baru.") {
            NotificationHelper.cancelAll(context, state.estimasi.keys.toList())
            doffVm.finishShift()
            shiftFinishedKey++
        }
    }

    val doffCount = state.aktual.size
    val totalMc = remember(state.estimasi, state.aktual) {
        (state.estimasi.keys + state.aktual.map { it.mcNo }).toSet().size
    }
    val aktualReversed = remember(state.aktual) { state.aktual.asReversed() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .systemBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Main scrollable content — scrolls behind the floating header & console card
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp,
                    top = 10.dp + headerHeight + 16.dp,
                    bottom = 10.dp + consoleBarHeight + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                permissionBanners(
                    notifGranted = notifGranted,
                    exactAlarmGranted = exactAlarmGranted,
                    batteryUnrestricted = batteryUnrestricted,
                    onNotifBannerClick = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onExactAlarmBannerClick = {
                        if (Build.VERSION.SDK_INT >= 31) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    },
                    onBatteryBannerClick = {
                        // On some OEM skins (e.g. OriginOS), tapping "Tetapkan sekarang" on
                        // the system dialog doesn't grant the exemption directly — it drops
                        // the user onto an app-battery-usage page where the real toggle is
                        // one tap deeper and defaults back to "Dioptimalkan". Walk the user
                        // through it explicitly instead of assuming the dialog alone works.
                        uiVm.showConfirm(
                            "Supaya notifikasi tidak telat, ikuti langkah ini (cukup sekali saja):\n\n" +
                                "1. Pada dialog berikutnya, ketuk \"Tetapkan sekarang\".\n" +
                                "2. Di halaman \"Penggunaan baterai aplikasi\", KETUK baris \"Izinkan penggunaan latar belakang\" (walau kelihatan sudah aktif).\n" +
                                "3. Pilih \"Tidak dibatasi\" (bukan \"Dioptimalkan\").",
                        ) {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    },
                )

                staleShiftBanner(
                    staleCount = if (staleShiftDismissed) 0 else staleDoffCount,
                    onFinishClick = { handleFinishShift() },
                    onDismiss = { staleShiftDismissed = true },
                )

                // The console's mode toggle doubles as a page switcher: ESTIMASI shows the
                // estimate rows, DOFFING shows the recorded-doff rows.
                when (mode) {
                    Mode.ESTIMASI -> {
                        estimasiSection(
                            radarList = radarList,
                            segeraList = segeraList,
                            menungguList = menungguList,
                            menungguRows = menungguRows,
                            menungguAccent = menungguAccent,
                            db = state.db,
                            nowAbs = nowAbs,
                            radarFilter = radarFilter,
                            onRadarFilterChange = { radarFilter = it },
                            onDoff = { mcNo -> handleDoff(mcNo) },
                            onHapus = { mcNo -> handleHapusEst(mcNo) },
                            onQuickEdit = { mcNo -> quickEditMcNo = mcNo },
                        )
                    }
                    Mode.AKTUAL -> {
                        doffingSection(
                            state = state,
                            aktualReversed = aktualReversed,
                            doffFilter = doffFilter,
                            onDoffFilterChange = { doffFilter = it },
                            onShare = { shareHistory(context, state) },
                            onStatistik = { statistikOpen = true },
                            onFinish = { handleFinishShift() },
                            onEntryClick = { id -> editAktId = id },
                            onHapusEntry = { id -> handleHapusAktual(id) },
                        )
                    }
                }
            }
        }

        // Header — floating card, overlays the list (list scrolls behind it)
        MainScreenHeader(
            nowAbs = nowAbs,
            totalMc = totalMc,
            doffCount = doffCount,
            showRemaining = showRemaining,
            onToggleShowRemaining = { showRemaining = !showRemaining },
            onGearClick = { settingsOpen = true },
            onHeightMeasured = { headerHeight = it },
            haptic = haptic,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
        )

        // Console command bar — floating card, overlays the list (list scrolls behind it)
        ConsoleBar(
            mode = mode,
            onModeSelect = { mode = it },
            input = input,
            onInputChange = { input = it },
            inputFocus = inputFocus,
            onSend = { handleCommand() },
            sendScale = sendScale.value,
            sendShowCheck = sendShowCheck,
            inputErrorFlash = inputErrorFlash,
            inputHint = inputHint,
            onHeightMeasured = { consoleBarHeight = it },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        )

        // Toast — floats just below the floating header. Anchored to the top (not the console
        // bar at the bottom) so the on-screen keyboard, which only ever covers the bottom of the
        // screen while typing a command, can never hide it right when it matters most.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = headerHeight + 8.dp),
        ) {
            ToastHost(toast = toast, onDismiss = { uiVm.dismissToast() })
        }

        // Settings panel — rendered in this same Box (not a separate Dialog window) so its own
        // AnimatedVisibility is the only thing animating it in/out, drawn last to sit on top.
        if (settingsOpen) {
            SettingsDrawer(
                state = state,
                onClose = { settingsOpen = false },
                onSetMesin = { mcNo, data -> doffVm.setMesin(mcNo, data) },
                onResetMesin = { mcNo -> doffVm.resetMesin(mcNo) },
                onResetDb = {
                    NotificationHelper.cancelAll(context, state.estimasi.keys.toList())
                    doffVm.resetDb()
                },
                onSetThemeMode = { mode -> doffVm.setThemeMode(mode.name) },
                onExportJson = { doffVm.exportJson() },
                onImport = { json ->
                    uiVm.showConfirm("Pulihkan data dari file ini? Semua data saat ini akan diganti.") {
                        val oldKeys = state.estimasi.keys.toList()
                        doffVm.importJson(json) { imported ->
                            if (imported != null) {
                                NotificationHelper.cancelAll(context, oldKeys)
                                val now = nowAbsMin()
                                imported.estimasi.values
                                    .filter { it.estAbsMin > now }
                                    .forEach { NotificationHelper.scheduleNotif(context, it.mcNo, it.estAbsMin) }
                                uiVm.showToast("Data dipulihkan ✓")
                            } else {
                                uiVm.showToast("⚠ File cadangan tidak valid")
                            }
                        }
                    }
                },
                showToast = { uiVm.showToast(it) },
                showConfirm = { msg, fn -> uiVm.showConfirm(msg, onConfirm = fn) },
            )
        }

        if (statistikOpen) {
            StatistikScreen(
                history = state.history,
                db = state.db,
                onClose = { statistikOpen = false },
                onDeleteShift = { id -> doffVm.hapusShift(id) },
                showConfirm = { msg, fn -> uiVm.showConfirm(msg, onConfirm = fn) },
            )
        }

        if (shiftFinishedVisible) {
            ShiftFinishedOverlay(checkScale = shiftCheckScale.value, backdropAlpha = shiftBackdropAlpha.value)
        }
    }

    // Overlays
    if (editAktId != null) {
        EditAktSheet(
            aktualId = editAktId,
            state = state,
            onClose = { editAktId = null },
            onSave = { id, ket, corakOverride, customYard ->
                doffVm.updateAktual(id, ket, corakOverride, customYard)
                uiVm.showToast("Riwayat diperbarui")
                editAktId = null
            },
            onInvalidYard = { uiVm.showToast("Yard tidak valid") },
            onEmptyKet = { uiVm.showToast("Keterangan tidak boleh kosong") },
            onDelete = { editAktId?.let { id -> handleHapusAktual(id) } },
        )
    }

    quickEditMcNo?.let { mcNo ->
        val mesin = state.db[mcNo] ?: MesinData()
        QuickEditCorakDialog(
            mcNo = mcNo,
            corak = mesin.corak,
            targetYard = mesin.targetYard,
            onDismiss = { quickEditMcNo = null },
            onSave = { corak, targetYard ->
                doffVm.setMesin(mcNo, mesin.copy(corak = corak, targetYard = targetYard))
                uiVm.showToast("Mc $mcNo disimpan ✓")
                quickEditMcNo = null
            },
        )
    }

    if (!state.onboardingSeen) {
        OnboardingDialog(onClose = { doffVm.setOnboardingSeen() })
    }

    ConfirmDialog(
        confirm = confirm,
        onDismiss = { uiVm.dismissConfirm() },
    )
}

@Composable
private fun ShiftFinishedOverlay(checkScale: Float, backdropAlpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f * backdropAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Emerald500,
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer { scaleX = checkScale; scaleY = checkScale; alpha = backdropAlpha },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Shift Selesai",
                style = AppType.DialogTitle.copy(color = Zinc100),
                modifier = Modifier.graphicsLayer { alpha = backdropAlpha },
            )
        }
    }
}

@Composable
private fun LoadingPlaceholder() {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Cyan500)
    }
}

@Composable
internal fun SectionHeader(title: String, count: Int) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = AppType.TabLabel.copy(letterSpacing = 0.5.sp, color = colors.textPrimary),
        )
        if (count > 0) {
            Text(
                text = "$count",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textFaint),
            )
        }
    }
}

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    title: String = "Belum ada mesin yang dipantau",
    subtitle: String = "Masukkan nomor mesin + estimasi di kolom bawah untuk mulai",
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(colors.bgElevated2.copy(alpha = 0.3f)))
            Box(Modifier.size(72.dp).clip(CircleShape).background(colors.bg))
            Box(Modifier.size(72.dp).clip(CircleShape).background(colors.bgElevated2.copy(alpha = 0.2f)))
            Box(Modifier.size(48.dp).clip(CircleShape).background(colors.bg))
            Box(Modifier.size(12.dp).clip(CircleShape).background(colors.border))
        }
        Text(
            text = title,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary),
        )
        Text(
            text = subtitle,
            style = AppType.Caption.copy(color = colors.textFaint, lineHeight = 17.sp),
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
