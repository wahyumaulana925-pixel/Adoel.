package com.jekael.adoel.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
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
    // Chosen once in Pengaturan (persisted in DoffState, like themeMode) rather than per-session —
    // switching it is rare enough that it doesn't need its own control on the main screen.
    val inputStyle = remember(state.inputStyle) {
        runCatching { InputStyle.valueOf(state.inputStyle) }.getOrDefault(InputStyle.TEKS)
    }

    // Console command bar — the one and only way to record estimasi/doffing. Saveable so a
    // rotation or process death mid-shift doesn't drop what the operator is in the middle of.
    var mode by rememberSaveable { mutableStateOf(Mode.AKTUAL) }
    var input by rememberSaveable { mutableStateOf("") }
    var radarFilter by rememberSaveable { mutableStateOf("") }
    var doffFilter by rememberSaveable { mutableStateOf("") }
    // Header's Bagikan/Statistik/Selesai Shift row — collapsed by default so the header stays
    // compact; available from both Estimasi and Doffing since the header itself is shared.
    var headerActionsExpanded by rememberSaveable { mutableStateOf(false) }

    val sendPulse = remember { SendPulseState() }
    LaunchedEffect(sendPulse.key) {
        if (sendPulse.key == 0) return@LaunchedEffect
        sendPulse.showCheck = true
        sendPulse.scale.snapTo(0.8f)
        launch {
            sendPulse.scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
        delay(500)
        sendPulse.showCheck = false
    }

    // Error feedback on a rejected console command — a toast alone (3.5s, auto-dismiss) can be
    // missed if the operator looks back at the machine right after hitting send, so this pairs it
    // with a haptic + a brief red ring around the input that doesn't depend on eyes staying on screen.
    val errorFlash = remember { ErrorFlashState() }
    LaunchedEffect(errorFlash.key) {
        if (errorFlash.key == 0) return@LaunchedEffect
        errorFlash.active = true
        delay(200)
        errorFlash.active = false
    }

    // "Nanti" on the stale-shift banner only snoozes it for this composition/app session — it's
    // not persisted, so if the shift genuinely never gets closed out, the reminder is back next
    // time the app is opened fresh.
    var staleShiftDismissed by remember { mutableStateOf(false) }

    // "Selesai Shift" closes out a full work shift — worth a beat more than a toast that's gone
    // in 3.5s, so this pops a big checkmark over a dimmed backdrop before fading on its own.
    val shiftFinished = remember { ShiftFinishedState() }
    LaunchedEffect(shiftFinished.key) {
        if (shiftFinished.key == 0) return@LaunchedEffect
        shiftFinished.visible = true
        shiftFinished.backdropAlpha.snapTo(0f)
        shiftFinished.checkScale.snapTo(0f)
        launch { shiftFinished.backdropAlpha.animateTo(1f, tween(150)) }
        shiftFinished.checkScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        delay(700)
        shiftFinished.backdropAlpha.animateTo(0f, tween(250))
        shiftFinished.visible = false
    }

    var nowAbs by remember { mutableLongStateOf(nowAbsMin()) }
    val permission = remember { PermissionState() }

    var activeOverlay by rememberSaveable(stateSaver = ActiveOverlaySaver) { mutableStateOf<ActiveOverlay>(ActiveOverlay.None) }
    var showRemaining by rememberSaveable { mutableStateOf(false) }

    val inputFocus = remember { FocusRequester() }
    var consoleBarHeight by remember { mutableStateOf(0.dp) }
    var headerHeight by remember { mutableStateOf(0.dp) }

    val handlers = remember(context, doffVm, uiVm, haptic) {
        MainScreenHandlers(context, doffVm, uiVm, haptic, sendPulse, errorFlash, shiftFinished)
    }

    // Request notification permission launcher
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permission.notifGranted = granted
        if (!granted) uiVm.showToast("⚠ Izin notifikasi ditolak")
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= 33) {
                    val nm = context.getSystemService(android.app.NotificationManager::class.java)
                    permission.notifGranted = nm.areNotificationsEnabled()
                }
                if (Build.VERSION.SDK_INT >= 31) {
                    val am = context.getSystemService(AlarmManager::class.java)
                    permission.exactAlarmGranted = am.canScheduleExactAlarms()
                }
                val powerManager = context.getSystemService(PowerManager::class.java)
                permission.batteryUnrestricted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
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
    // Derived (not a plain remember(nowAbs, ...)) — partitioning only needs to actually re-propagate
    // to whatever reads segeraList/menungguList when a card crosses the Segera/Menunggu boundary,
    // not on every 5-second nowAbs tick that leaves the partition unchanged.
    val (segeraList, menungguList) = remember(filteredRadarList) {
        derivedStateOf { partitionSegeraMenunggu(filteredRadarList, nowAbs) }
    }.value
    // Real-time reminder of the expected command syntax for the machine number being typed —
    // TAPPET/CAM want a duration, D405 wants running yard, D408 wants a jam counter — so an
    // operator who forgets the format gets a hint before submitting instead of a generic error.
    val inputHint = remember(input, mode, state.db) {
        if (mode != Mode.ESTIMASI) return@remember null
        val mcNo = input.trim().substringBefore(' ')
        if (mcNo.isEmpty() || !mcNo.matches(Regex("^\\d{1,3}$"))) return@remember null
        val tipe = state.db[mcNo]?.tipe ?: return@remember null
        val hint = estimasiFieldHint(tipe)
        "Mc $mcNo → ${hint.label.replaceFirstChar { it.lowercase() }}, cth: $mcNo ${hint.example}"
    }
    // Flags doff entries left over from a shift the operator forgot to close via "Selesai Shift"
    // before the next 06.00/14.00/22.00 boundary — otherwise they'd silently get archived together
    // with the new shift's entries the next time Selesai Shift is pressed. Derived so this only
    // re-propagates when the stale count itself changes, not on every 5-second tick.
    val staleDoffCount by remember(state.aktual) {
        derivedStateOf {
            val shiftStart = currentShiftStartAbsMin(nowAbs)
            state.aktual.count { val ts = it.tsEpochMin; ts != null && ts < shiftStart }
        }
    }
    // Un-snooze once the stale batch is actually resolved (e.g. via Selesai Shift), so a *later*
    // forgotten shift isn't silently suppressed by a "Nanti" tap from a previous, unrelated one.
    LaunchedEffect(staleDoffCount == 0) {
        if (staleDoffCount == 0) staleShiftDismissed = false
    }
    // Menunggu bucket spans CALM through IMMINENT (Segera already claims OVERDUE) — tint the
    // band header by its most urgent member so it doesn't read "calm" while cards inside are
    // already amber/orange. Derived so this only re-propagates when the accent color itself changes.
    val menungguAccent by remember(menungguList) {
        derivedStateOf {
            when (menungguList.maxOfOrNull { urgencyLevel(it.estAbsMin - nowAbs) }) {
                UrgencyLevel.IMMINENT -> Orange400
                UrgencyLevel.SOON -> Amber400
                else -> Cyan400
            }
        }
    }
    // Flag long idle stretches between two upcoming doffs so the operator knows when it's
    // actually safe to step away, instead of having to eyeball the gap between two times.
    val menungguRows = remember(menungguList, segeraList, nowAbs) {
        buildList {
            // Leading break: a gap card used to anchor to whichever machine sat just before it
            // in the list — doffing/menghapus that machine removed it from menungguList, which
            // silently swallowed the card even though the free time itself hadn't gone anywhere
            // (it's just measured from "now" instead of from that machine's estimate). Only valid
            // when nothing is overdue (Segera empty) — an operator with something already due
            // doesn't have free time to call out yet.
            val first = menungguList.firstOrNull()
            if (segeraList.isEmpty() && first != null) {
                val gap = first.estAbsMin - nowAbs
                if (gap >= BREAK_GAP_THRESHOLD_MIN) {
                    add(MenungguRow.GapRow(afterMcNo = "now", nextMcNo = first.mcNo, gapMin = gap, nextAbsMin = first.estAbsMin))
                }
            }
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
                    notifGranted = permission.notifGranted,
                    exactAlarmGranted = permission.exactAlarmGranted,
                    batteryUnrestricted = permission.batteryUnrestricted,
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
                    onFinishClick = { handlers.handleFinishShift() },
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
                            onDoff = { mcNo ->
                                if (inputStyle == InputStyle.TERPANDU) {
                                    activeOverlay = ActiveOverlay.GuidedDoffing(mcNo)
                                } else {
                                    handlers.handleDoff(mcNo)
                                }
                            },
                            onHapus = { mcNo -> handlers.handleHapusEst(mcNo) },
                            onQuickEdit = { mcNo ->
                                activeOverlay = if (inputStyle == InputStyle.TERPANDU) {
                                    ActiveOverlay.GuidedEstimasi(mcNo)
                                } else {
                                    ActiveOverlay.QuickEditMesin(mcNo)
                                }
                            },
                        )
                    }
                    Mode.AKTUAL -> {
                        doffingSection(
                            state = state,
                            aktualReversed = aktualReversed,
                            doffFilter = doffFilter,
                            onDoffFilterChange = { doffFilter = it },
                            onEntryClick = { id -> activeOverlay = ActiveOverlay.EditAkt(id) },
                            onHapusEntry = { id -> handlers.handleHapusAktual(id) { activeOverlay = ActiveOverlay.None } },
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
            onGearClick = { activeOverlay = ActiveOverlay.Settings },
            actionsExpanded = headerActionsExpanded,
            onToggleActionsExpanded = { headerActionsExpanded = !headerActionsExpanded },
            onShare = { shareHistory(context, state) },
            onStatistik = { activeOverlay = ActiveOverlay.Statistik },
            onFinish = { handlers.handleFinishShift() },
            onHeightMeasured = { headerHeight = it },
            haptic = haptic,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
        )

        // Console command bar — floating card, overlays the list (list scrolls behind it)
        ConsoleBar(
            mode = mode,
            onModeSelect = { mode = it },
            inputStyle = inputStyle,
            input = input,
            onInputChange = { input = it },
            inputFocus = inputFocus,
            onSend = { handlers.handleCommand(mode, input) { input = "" } },
            sendScale = { sendPulse.scale.value },
            sendShowCheck = sendPulse.showCheck,
            inputErrorFlash = errorFlash.active,
            inputHint = inputHint,
            onGuidedStart = { mcNo ->
                if (state.db[mcNo] == null) {
                    uiVm.showToast("⚠ Mc $mcNo tidak ditemukan")
                } else {
                    activeOverlay = if (mode == Mode.ESTIMASI) {
                        ActiveOverlay.GuidedEstimasi(mcNo)
                    } else {
                        ActiveOverlay.GuidedDoffing(mcNo)
                    }
                }
            },
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
        if (activeOverlay is ActiveOverlay.Settings) {
            SettingsDrawer(
                state = state,
                onClose = { activeOverlay = ActiveOverlay.None },
                onSetMesin = { mcNo, data -> doffVm.setMesin(mcNo, data) },
                onResetMesin = { mcNo -> doffVm.resetMesin(mcNo) },
                onResetDb = {
                    NotificationHelper.cancelAll(context, state.estimasi.keys.toList())
                    doffVm.resetDb()
                },
                onSetThemeMode = { mode -> doffVm.setThemeMode(mode.name) },
                onSetInputStyle = { style -> doffVm.setInputStyle(style.name) },
                onExportJson = { doffVm.exportJson() },
                onImport = { json ->
                    uiVm.showConfirm("Pulihkan data dari file ini? Semua data saat ini akan diganti.") {
                        val oldKeys = state.estimasi.keys.toList()
                        doffVm.importJson(json) { imported ->
                            if (imported != null) {
                                NotificationHelper.cancelAll(context, oldKeys)
                                NotificationHelper.rescheduleAll(context, imported.estimasi.values)
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

        if (activeOverlay is ActiveOverlay.Statistik) {
            StatistikScreen(
                history = state.history,
                db = state.db,
                onClose = { activeOverlay = ActiveOverlay.None },
                onDeleteShift = { id -> doffVm.hapusShift(id) },
                showConfirm = { msg, fn -> uiVm.showConfirm(msg, onConfirm = fn) },
            )
        }

        if (shiftFinished.visible) {
            ShiftFinishedOverlay(
                checkScale = { shiftFinished.checkScale.value },
                backdropAlpha = { shiftFinished.backdropAlpha.value },
            )
        }
    }

    // Overlays
    val editAktId = (activeOverlay as? ActiveOverlay.EditAkt)?.id
    // activeOverlay is rememberSaveable now, so a restored EditAkt can point at an entry that no
    // longer exists (e.g. deleted from another device before this process was recreated) — reset
    // instead of leaving the overlay stuck non-None with nothing to show for it.
    LaunchedEffect(editAktId, state.aktual) {
        if (editAktId != null && state.aktual.none { it.id == editAktId }) {
            activeOverlay = ActiveOverlay.None
        }
    }
    if (editAktId != null && state.aktual.any { it.id == editAktId }) {
        EditAktSheet(
            aktualId = editAktId,
            state = state,
            onClose = { activeOverlay = ActiveOverlay.None },
            onSave = { id, ket, corakOverride, customYard ->
                doffVm.updateAktual(id, ket, corakOverride, customYard)
                uiVm.showToast("Riwayat diperbarui")
                activeOverlay = ActiveOverlay.None
            },
            onInvalidYard = { uiVm.showToast("Yard tidak valid") },
            onEmptyKet = { uiVm.showToast("Keterangan tidak boleh kosong") },
            onDelete = { handlers.handleHapusAktual(editAktId) { activeOverlay = ActiveOverlay.None } },
        )
    }

    val quickEditMcNo = (activeOverlay as? ActiveOverlay.QuickEditMesin)?.mcNo
    if (quickEditMcNo != null) {
        val mesin = state.db[quickEditMcNo] ?: MesinData()
        QuickEditCorakDialog(
            mcNo = quickEditMcNo,
            corak = mesin.corak,
            targetYard = mesin.targetYard,
            onDismiss = { activeOverlay = ActiveOverlay.None },
            onSave = { corak, targetYard ->
                doffVm.setMesin(quickEditMcNo, mesin.copy(corak = corak, targetYard = targetYard))
                uiVm.showToast("Mc $quickEditMcNo disimpan ✓")
                activeOverlay = ActiveOverlay.None
            },
        )
    }

    val guidedEstimasiMcNo = (activeOverlay as? ActiveOverlay.GuidedEstimasi)?.mcNo
    if (guidedEstimasiMcNo != null) {
        GuidedEstimasiSheet(
            mcNo = guidedEstimasiMcNo,
            mesin = state.db[guidedEstimasiMcNo],
            onDismiss = { activeOverlay = ActiveOverlay.None },
            onSubmit = { value ->
                handlers.handleCommand(Mode.ESTIMASI, value) {
                    activeOverlay = ActiveOverlay.None
                }
            },
        )
    }

    val guidedDoffingMcNo = (activeOverlay as? ActiveOverlay.GuidedDoffing)?.mcNo
    if (guidedDoffingMcNo != null) {
        GuidedDoffingSheet(
            mcNo = guidedDoffingMcNo,
            mesin = state.db[guidedDoffingMcNo],
            estimasi = state.estimasi[guidedDoffingMcNo],
            onDismiss = { activeOverlay = ActiveOverlay.None },
            onSubmitDoffing = { value ->
                handlers.handleCommand(Mode.AKTUAL, value) {
                    activeOverlay = ActiveOverlay.None
                }
            },
            onSubmitCounterUpdate = { value ->
                handlers.handleCommand(Mode.ESTIMASI, value) {
                    activeOverlay = ActiveOverlay.None
                }
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
