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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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

private enum class Mode { ESTIMASI, AKTUAL }

@Composable
fun MainScreen(
    doffVm: DoffViewModel,
    uiVm: UIViewModel,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colors = LocalAppColors.current
    val state by doffVm.state.collectAsStateWithLifecycle()
    val toast by uiVm.toast.collectAsStateWithLifecycle()
    val confirm by uiVm.confirm.collectAsStateWithLifecycle()

    // Console command bar — the one and only way to record estimasi/doffing
    var mode by remember { mutableStateOf(Mode.AKTUAL) }
    var input by remember { mutableStateOf("") }

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

    var nowAbs by remember { mutableLongStateOf(nowAbsMin()) }
    var notifGranted by remember { mutableStateOf(true) }
    var exactAlarmGranted by remember { mutableStateOf(true) }
    var batteryUnrestricted by remember { mutableStateOf(true) }

    var settingsOpen by remember { mutableStateOf(false) }
    var statistikOpen by remember { mutableStateOf(false) }
    var editAktId by remember { mutableStateOf<Int?>(null) }
    var showRemaining by remember { mutableStateOf(false) }

    val inputFocus = remember { FocusRequester() }
    val density = LocalDensity.current
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
    val (segeraList, menungguList) = remember(radarList, nowAbs) {
        partitionSegeraMenunggu(radarList, nowAbs)
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
    fun handleCommand() {
        val cmd = input.trim().uppercase()
        if (cmd.isEmpty()) return

        when (mode) {
            Mode.ESTIMASI -> {
                val result = doffVm.prosesBarisKondisiMesin(cmd, nowAbsMin())
                when (result) {
                    is ProsesResult.Ok -> {
                        sendPulseKey++
                        uiVm.showToast(result.msg)
                        input = ""
                        result.estAbs?.let { NotificationHelper.scheduleNotif(context, result.mcNo, it) }
                    }
                    is ProsesResult.Err -> uiVm.showToast("⚠ ${result.msg}")
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
                    is ProsesResult.Err -> uiVm.showToast("⚠ ${result.msg}")
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

    val doffCount = state.aktual.size
    val totalMc = remember(state.estimasi, state.aktual) {
        (state.estimasi.keys + state.aktual.map { it.mcNo }).toSet().size
    }

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
                if (!notifGranted) {
                    item {
                        TextButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= 33) {
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().animateItem(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = colors.bannerWarnBg,
                                contentColor = colors.bannerWarnFg,
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text("Notifikasi nonaktif — ketuk untuk izinkan", style = TextStyle(fontSize = 12.sp))
                        }
                    }
                }

                if (notifGranted && !exactAlarmGranted) {
                    item {
                        TextButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= 31) {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    runCatching { context.startActivity(intent) }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().animateItem(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = colors.bannerWarnBg,
                                contentColor = colors.bannerWarnFg,
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text("Izin alarm tepat waktu nonaktif — ketuk untuk izinkan (wajib agar notifikasi doff tepat waktu)", style = TextStyle(fontSize = 12.sp))
                        }
                    }
                }

                if (notifGranted && exactAlarmGranted && !batteryUnrestricted) {
                    item {
                        TextButton(
                            onClick = {
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
                            modifier = Modifier.fillMaxWidth().animateItem(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = colors.bannerWarnBg,
                                contentColor = colors.bannerWarnFg,
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text("Baterai dioptimalkan — ketuk agar notifikasi tidak diblokir sistem", style = TextStyle(fontSize = 12.sp))
                        }
                    }
                }

                // The console's mode toggle doubles as a page switcher: ESTIMASI shows the
                // estimate rows, DOFFING shows the recorded-doff rows.
                when (mode) {
                    Mode.ESTIMASI -> {
                        item(key = "est_header") {
                            SectionHeader(title = "Estimasi", count = radarList.size)
                        }
                        if (radarList.isEmpty()) {
                            item(key = "est_empty") {
                                EmptyState(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp))
                            }
                        } else {
                            if (segeraList.isNotEmpty()) {
                                item(key = "segera_head") {
                                    UrgencyBandHeader(label = "Segera", color = Red400, modifier = Modifier.animateItem())
                                }
                                items(segeraList, key = { it.mcNo }) { est ->
                                    RadarCard(
                                        est = est,
                                        mesin = state.db[est.mcNo],
                                        nowAbs = nowAbs,
                                        onDoff = { handleDoff(est.mcNo) },
                                        onHapus = { handleHapusEst(est.mcNo) },
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                            }
                            if (menungguList.isNotEmpty()) {
                                item(key = "menunggu_head") {
                                    UrgencyBandHeader(label = "Menunggu", color = menungguAccent, modifier = Modifier.animateItem())
                                }
                                items(menungguList, key = { it.mcNo }) { est ->
                                    RadarCard(
                                        est = est,
                                        mesin = state.db[est.mcNo],
                                        nowAbs = nowAbs,
                                        onDoff = { handleDoff(est.mcNo) },
                                        onHapus = { handleHapusEst(est.mcNo) },
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                            }
                        }
                    }
                    Mode.AKTUAL -> {
                        item(key = "doff_header") {
                            SectionHeader(title = "Doffing", count = state.aktual.size)
                        }
                        // Always shown — Statistik reads state.history, which survives even when
                        // the live aktual list is empty right after "Selesai Shift".
                        item(key = "doff_actions") {
                            DoffingActions(
                                onShare = { shareHistory(context, state) },
                                onStatistik = { statistikOpen = true },
                                onFinish = {
                                    uiVm.showConfirm("Akhiri shift? ${state.aktual.size} doff & ${state.estimasi.size} estimasi akan diarsipkan ke Riwayat, lalu konsol dikosongkan untuk shift baru.") {
                                        NotificationHelper.cancelAll(context, state.estimasi.keys.toList())
                                        doffVm.finishShift()
                                        uiVm.showToast("Shift selesai ✓")
                                    }
                                },
                            )
                        }
                        if (state.aktual.isEmpty()) {
                            item(key = "doff_empty") {
                                EmptyState(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                    title = "Belum ada doff",
                                    subtitle = "Doff akan muncul di sini setelah kamu proses baris di ESTIMASI/AKTUAL",
                                )
                            }
                        } else {
                            itemsIndexed(state.aktual.asReversed(), key = { _, e -> e.id }) { idx, entry ->
                                DoffingRow(
                                    entry = entry,
                                    mesin = state.db[entry.mcNo],
                                    num = idx + 1,
                                    onClick = { editAktId = entry.id },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Header — floating card, overlays the list (list scrolls behind it)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    headerHeight = with(density) { coords.size.height.toDp() }
                }
                .padding(horizontal = 12.dp)
                .padding(top = 12.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(colors.bgElevated),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Branding — a one-shot ~320ms tap pulse: logo settles to 0.97, the dot hops up
                // 7dp and a thin glow blooms from it, all finishing on their own (not tied to how
                // long the finger stays down) so repeated daily taps stay quick and subtle.
                var brandPulseKey by remember { mutableStateOf(0) }
                val brandScale = remember { Animatable(1f) }
                val dotOffsetY = remember { Animatable(0f) }
                val glowAlpha = remember { Animatable(0f) }
                val glowScale = remember { Animatable(0.6f) }
                LaunchedEffect(brandPulseKey) {
                    if (brandPulseKey == 0) return@LaunchedEffect
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    launch {
                        brandScale.animateTo(0.97f, tween(90, easing = FastOutSlowInEasing))
                        brandScale.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
                    }
                    launch {
                        dotOffsetY.animateTo(-7f, tween(120, easing = FastOutSlowInEasing))
                        dotOffsetY.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                    }
                    launch {
                        glowScale.snapTo(0.6f)
                        glowAlpha.snapTo(0.4f)
                        launch { glowAlpha.animateTo(0f, tween(300, easing = LinearOutSlowInEasing)) }
                        glowScale.animateTo(2.2f, tween(320, easing = LinearOutSlowInEasing))
                    }
                }
                val shiftLabel = remember(nowAbs) {
                    val cal = Calendar.getInstance().apply { timeInMillis = nowAbs * 60000L }
                    "Shift ${shiftNumberForEpochMin(nowAbs)} · %02d/%02d".format(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .graphicsLayer { scaleX = brandScale.value; scaleY = brandScale.value }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { brandPulseKey++ },
                    ) {
                        Text(
                            text = "Adoel",
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Black, color = colors.textPrimary, letterSpacing = (-0.5).sp),
                        )
                        Text(
                            text = ".",
                            modifier = Modifier
                                .offset(y = dotOffsetY.value.dp)
                                .drawBehind {
                                    drawCircle(
                                        color = Amber500.copy(alpha = glowAlpha.value),
                                        radius = (size.minDimension.coerceAtLeast(20f)) * glowScale.value,
                                        center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                                    )
                                },
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Black, color = Amber500),
                        )
                    }
                    Text(
                        text = shiftLabel,
                        style = TextStyle(fontSize = 9.sp, color = colors.textFaint),
                    )
                }

                // Shift progress — centered between branding and the menu icon
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (totalMc > 0) {
                        val remainingMc = totalMc - doffCount
                        val shiftFraction = doffCount.toFloat() / totalMc
                        val animatedFraction by animateFloatAsState(
                            targetValue = shiftFraction.coerceIn(0f, 1f),
                            animationSpec = tween(400),
                            label = "shiftProgress",
                        )
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showRemaining = !showRemaining
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = when {
                                    !showRemaining -> "$doffCount/$totalMc"
                                    remainingMc <= 0 -> "Selesai"
                                    else -> "$remainingMc lagi"
                                },
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Cyan400),
                            )
                            Spacer(Modifier.height(4.dp))
                            LinearProgressBar(
                                fraction = animatedFraction,
                                trackColor = colors.bgElevated2,
                                fillColor = Cyan500,
                            )
                        }
                    }
                }

                // Gear button
                IconButton(
                    onClick = { settingsOpen = true },
                ) {
                    GearIcon()
                }
            }
        }

        // Console command bar — floating card, overlays the list (list scrolls behind it).
        // imePadding() sits outside the card's own shape/shadow/margin so the whole card
        // rises above the keyboard as one floating unit instead of fusing flush to it.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .onGloballyPositioned { coords ->
                    consoleBarHeight = with(density) { coords.size.height.toDp() }
                }
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(colors.bgElevated),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 10.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp),
            ) {
                // Mode toggle
                SlidingToggle(
                    labelLeft = "ESTIMASI",
                    labelRight = "DOFFING",
                    selectedIndex = if (mode == Mode.ESTIMASI) 0 else 1,
                    onSelect = { mode = if (it == 0) Mode.ESTIMASI else Mode.AKTUAL },
                    containerColor = colors.bgElevated2,
                    activeColorLeft = Amber500,
                    activeColorRight = Cyan600,
                    activeTextColorLeft = Zinc950,
                    activeTextColorRight = Zinc100,
                    inactiveTextColor = colors.textMuted,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))

                // Command row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.uppercase() },
                        modifier = Modifier.weight(1f).focusRequester(inputFocus),
                        placeholder = {
                            Text(
                                if (mode == Mode.ESTIMASI) "cth: 31 45" else "cth: 31 HB",
                                color = colors.textFaint,
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan500,
                            unfocusedBorderColor = colors.border,
                            cursorColor = Cyan500,
                            focusedContainerColor = colors.bgElevated2,
                            unfocusedContainerColor = colors.bgElevated2,
                        ),
                        shape = RoundedCornerShape(50.dp),
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-0.5).sp,
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Send,
                        ),
                        keyboardActions = KeyboardActions(onSend = { handleCommand() }),
                        singleLine = true,
                    )
                    // Send button — bounces and briefly flashes a checkmark on a successful submit
                    Button(
                        onClick = { handleCommand() },
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer { scaleX = sendScale.value; scaleY = sendScale.value }
                            .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = Cyan600.copy(alpha = 0.6f)),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = if (sendShowCheck) Emerald500 else Cyan600),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Crossfade(targetState = sendShowCheck, label = "sendIcon") { showCheck ->
                            if (showCheck) CheckIcon() else SendIcon()
                        }
                    }
                }
            }
        }

        // Toast — floats just above the floating console card, never covers it
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = consoleBarHeight + 8.dp),
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
private fun SectionHeader(title: String, count: Int) {
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
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = colors.textPrimary),
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
private fun DoffingActions(onShare: () -> Unit, onStatistik: () -> Unit, onFinish: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onShare,
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
            border = BorderStroke(1.dp, colors.border),
        ) { Text("Bagikan", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)) }
        OutlinedButton(
            onClick = onStatistik,
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
            border = BorderStroke(1.dp, colors.border),
        ) { Text("Statistik", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)) }
        OutlinedButton(
            onClick = onFinish,
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
            border = BorderStroke(1.dp, Red700.copy(alpha = 0.5f)),
        ) { Text("Selesai Shift", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)) }
    }
}

@Composable
private fun DoffingRow(
    entry: AktualEntry,
    mesin: MesinData?,
    num: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val corak = entry.corakOverride ?: mesin?.corak ?: "—"
    val sub = when {
        entry.customYard != null -> "$corak · ${formatYard(entry.customYard)}y"
        mesin?.targetYard != null -> "$corak · ${formatYard(mesin.targetYard)}y"
        else -> corak
    }
    val dotColor = when (mesin?.tipe) {
        MesinTipe.TAPPET -> Teal500
        MesinTipe.CAM -> Violet500
        MesinTipe.D405 -> Amber500
        MesinTipe.D408 -> Sky500
        null -> colors.textFaint
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.bgElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "$num",
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Black, color = colors.textMuted),
            modifier = Modifier.width(22.dp),
        )
        Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.mcNo,
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, color = Cyan500, letterSpacing = (-1).sp),
            )
            Text(
                text = sub,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = colors.textMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = entry.ket,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary),
        )
    }
}

@Composable
private fun UrgencyBandHeader(label: String, color: Color, modifier: Modifier = Modifier) {
    val animatedColor by animateColorAsState(color, animationSpec = tween(300), label = "urgencyBandColor")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(animatedColor),
            )
            Text(
                text = label,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = animatedColor),
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
            style = TextStyle(fontSize = 12.sp, color = colors.textFaint, lineHeight = 17.sp),
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

private fun shareHistory(context: Context, state: DoffState) {
    val cal = Calendar.getInstance()
    val dateStr = "%02d/%02d/%04d".format(
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.YEAR),
    )
    val lines = state.aktual.asReversed().mapIndexed { i, a ->
        val mesin = state.db[a.mcNo]
        val corak = a.corakOverride ?: mesin?.corak ?: "—"
        val yard = a.customYard ?: mesin?.targetYard
        val suffix = if (yard != null) " [${formatYard(yard)}y]" else ""
        "${i + 1}. Mc${a.mcNo} - $corak$suffix - ${a.ket}"
    }
    val text = "Bravo!!!\n$dateStr\n\n${lines.joinToString("\n")}\n\nTotal: ${state.aktual.size} doff"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Bagikan riwayat")) }
}
