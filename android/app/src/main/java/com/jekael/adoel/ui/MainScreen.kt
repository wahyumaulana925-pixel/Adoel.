package com.jekael.adoel.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jekael.adoel.data.*
import com.jekael.adoel.notification.NotificationHelper
import com.jekael.adoel.ui.components.*
import com.jekael.adoel.ui.theme.*
import com.jekael.adoel.viewmodel.DoffViewModel
import com.jekael.adoel.viewmodel.UIViewModel
import kotlinx.coroutines.delay
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

    var nowAbs by remember { mutableLongStateOf(nowAbsMin()) }
    var notifGranted by remember { mutableStateOf(true) }

    var historyOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var editAktId by remember { mutableStateOf<Int?>(null) }
    var historyExpanded by remember { mutableStateOf(false) }
    var showRemaining by remember { mutableStateOf(false) }
    var segeraExpanded by remember { mutableStateOf(true) }
    var menungguExpanded by remember { mutableStateOf(true) }

    val inputFocus = remember { FocusRequester() }
    val density = LocalDensity.current
    var consoleBarHeight by remember { mutableStateOf(0.dp) }

    // Request notification permission launcher
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
        if (!granted) uiVm.showToast("⚠ Izin notifikasi ditolak")
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            val pm = context.getSystemService(android.app.NotificationManager::class.java)
            notifGranted = pm.areNotificationsEnabled()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            nowAbs = nowAbsMin()
        }
    }

    val radarList = remember(state.estimasi) {
        state.estimasi.values.sortedBy { it.estAbsMin }
    }
    val (segeraList, menungguList) = remember(radarList, nowAbs) {
        radarList.partition { it.estAbsMin - nowAbs <= 0 }
    }
    val recentHistory = remember(state.aktual) {
        state.aktual.take(5)
    }

    fun handleCommand() {
        val cmd = input.trim().uppercase()
        if (cmd.isEmpty()) return

        when (mode) {
            Mode.ESTIMASI -> {
                val result = doffVm.prosesBarisKondisiMesin(cmd, nowAbsMin())
                when (result) {
                    is ProsesResult.Ok -> {
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

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Branding
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Adoel",
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Black, color = colors.textPrimary, letterSpacing = (-0.5).sp),
                    )
                    Text(
                        text = ".",
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Black, color = Cyan400),
                    )
                }
                // Gear button
                IconButton(
                    onClick = { settingsOpen = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.bgElevated, CircleShape),
                ) {
                    GearIcon()
                }
            }

            HorizontalDivider(color = colors.bgElevated2.copy(alpha = 0.6f))

            // Shift progress — persistent framing of how far along the current shift is
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
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showRemaining = !showRemaining
                        }
                        .background(colors.bg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = if (showRemaining) "Sisa doff" else "Progres shift",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, color = colors.textMuted),
                        )
                        Text(
                            text = if (showRemaining) "$remainingMc mesin lagi" else "$doffCount dari $totalMc selesai",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Cyan400),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.bgElevated2),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFraction)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Cyan500),
                        )
                    }
                }
                HorizontalDivider(color = colors.bgElevated2.copy(alpha = 0.6f))
            }

            // Notification banner
            if (!notifGranted) {
                TextButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = colors.bannerWarnBg,
                        contentColor = colors.bannerWarnFg,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("Notifikasi nonaktif — ketuk untuk izinkan", style = TextStyle(fontSize = 12.sp))
                }
                HorizontalDivider(color = Amber700.copy(alpha = 0.5f))
            }

            // Main scrollable content — scrolls behind the floating console card
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp, top = 10.dp,
                    bottom = 10.dp + consoleBarHeight + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    SectionHeader(title = "Mesin Siap", count = radarList.size)
                }

                if (radarList.isEmpty()) {
                    item {
                        EmptyState(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp))
                    }
                } else {
                    if (segeraList.isNotEmpty()) {
                        item {
                            UrgencyBandHeader(
                                label = "Segera", count = segeraList.size, color = Red400,
                                expanded = segeraExpanded, onToggle = { segeraExpanded = !segeraExpanded },
                            )
                        }
                        if (segeraList.size <= 1 || segeraExpanded) {
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
                        } else {
                            item(key = "segera_stack") {
                                val front = segeraList.first()
                                RadarStackedPeek(
                                    front = front,
                                    mesin = state.db[front.mcNo],
                                    nowAbs = nowAbs,
                                    peekCount = segeraList.size - 1,
                                    accent = Red400,
                                    onDoff = { handleDoff(front.mcNo) },
                                    onHapus = { handleHapusEst(front.mcNo) },
                                    onExpand = { segeraExpanded = true },
                                )
                            }
                        }
                    }
                    if (menungguList.isNotEmpty()) {
                        item {
                            UrgencyBandHeader(
                                label = "Menunggu", count = menungguList.size, color = Cyan400,
                                expanded = menungguExpanded, onToggle = { menungguExpanded = !menungguExpanded },
                            )
                        }
                        if (menungguList.size <= 1 || menungguExpanded) {
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
                        } else {
                            item(key = "menunggu_stack") {
                                val front = menungguList.first()
                                RadarStackedPeek(
                                    front = front,
                                    mesin = state.db[front.mcNo],
                                    nowAbs = nowAbs,
                                    peekCount = menungguList.size - 1,
                                    accent = Cyan400,
                                    onDoff = { handleDoff(front.mcNo) },
                                    onHapus = { handleHapusEst(front.mcNo) },
                                    onExpand = { menungguExpanded = true },
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(12.dp))
                    HistorySectionHeader(
                        count = state.aktual.size,
                        expanded = historyExpanded,
                        onToggle = { historyExpanded = !historyExpanded },
                    )
                }

                if (recentHistory.isEmpty()) {
                    if (historyExpanded) {
                        item {
                            Text(
                                text = "Belum ada doff hari ini",
                                style = TextStyle(fontSize = 13.sp, color = colors.textFaint),
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    }
                } else if (historyExpanded) {
                    items(recentHistory, key = { "hist_${it.id}" }) { entry ->
                        HistoryPreviewRow(
                            entry = entry,
                            mesin = state.db[entry.mcNo],
                            onClick = { editAktId = entry.id },
                        )
                    }
                    item {
                        TextButton(
                            onClick = { historyOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "Lihat semua riwayat →",
                                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Cyan400),
                            )
                        }
                    }
                } else {
                    item(key = "history_stack") {
                        val front = recentHistory.first()
                        HistoryStackedPeek(
                            front = front,
                            mesin = state.db[front.mcNo],
                            peekCount = recentHistory.size - 1,
                            onClick = { editAktId = front.id },
                            onExpand = { historyExpanded = true },
                        )
                    }
                }
            }
        }

        // Console command bar — floating card, overlays the list (list scrolls behind it)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    consoleBarHeight = with(density) { coords.size.height.toDp() }
                }
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(colors.bgElevated)
                .imePadding(),
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bgElevated2, RoundedCornerShape(50.dp))
                        .padding(4.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { mode = Mode.ESTIMASI },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mode == Mode.ESTIMASI) Amber500 else Color.Transparent,
                                contentColor = if (mode == Mode.ESTIMASI) Zinc950 else colors.textMuted,
                            ),
                            contentPadding = PaddingValues(vertical = 10.dp),
                            elevation = ButtonDefaults.buttonElevation(0.dp),
                        ) {
                            Text("ESTIMASI", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold))
                        }
                        Button(
                            onClick = { mode = Mode.AKTUAL },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mode == Mode.AKTUAL) Cyan600 else Color.Transparent,
                                contentColor = if (mode == Mode.AKTUAL) Zinc100 else colors.textMuted,
                            ),
                            contentPadding = PaddingValues(vertical = 10.dp),
                            elevation = ButtonDefaults.buttonElevation(0.dp),
                        ) {
                            Text("DOFFING", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }

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
                    // Send button
                    Button(
                        onClick = { handleCommand() },
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
                        contentPadding = PaddingValues(0.dp),
                    ) { SendIcon() }
                    // History button
                    Button(
                        onClick = { historyOpen = true },
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.bgElevated2, contentColor = colors.textSecondary),
                        contentPadding = PaddingValues(0.dp),
                    ) { HistoryIcon() }
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
    }

    // Overlays
    if (historyOpen) {
        HistoryDrawer(
            state = state,
            onClose = { historyOpen = false },
            onEditAkt = { id -> historyOpen = false; editAktId = id },
            onShare = { shareHistory(context, state) },
            onFinishShift = {
                uiVm.showConfirm("Akhiri shift? Riwayat ${state.aktual.size} doff dan ${state.estimasi.size} estimasi akan dihapus.") {
                    NotificationHelper.cancelAll(context, state.estimasi.keys.toList())
                    doffVm.finishShift()
                    historyOpen = false
                    uiVm.showToast("Shift selesai ✓")
                }
            },
        )
    }

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
            showToast = { uiVm.showToast(it) },
            showConfirm = { msg, fn -> uiVm.showConfirm(msg, onConfirm = fn) },
        )
    }

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
        )
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
private fun UrgencyBandHeader(label: String, count: Int, color: Color, expanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "urgencyChevron",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = count > 1, onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = label,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = color),
            )
        }
        if (count > 1) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Ciutkan" else "Perluas",
                tint = color,
                modifier = Modifier.size(16.dp).rotate(rotation),
            )
        }
    }
}

@Composable
private fun PeekBars(count: Int, accent: Color) {
    val bars = count.coerceIn(0, 2)
    if (bars == 0) return
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(3.dp))
        for (i in 1..bars) {
            val widthFraction = (1f - i * 0.05f).coerceIn(0.7f, 1f)
            val barAlpha = (0.22f - (i - 1) * 0.10f).coerceAtLeast(0.06f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(10.dp)
                    .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                    .background(accent.copy(alpha = barAlpha)),
            )
            if (i != bars) Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
private fun RadarStackedPeek(
    front: Estimasi,
    mesin: MesinData?,
    nowAbs: Long,
    peekCount: Int,
    accent: Color,
    onDoff: () -> Unit,
    onHapus: () -> Unit,
    onExpand: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RadarCard(est = front, mesin = mesin, nowAbs = nowAbs, onDoff = onDoff, onHapus = onHapus)
        if (peekCount > 0) {
            PeekBars(count = peekCount, accent = accent)
            TextButton(onClick = onExpand, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "+$peekCount lainnya",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent),
                )
            }
        }
    }
}

@Composable
private fun HistoryStackedPeek(
    front: AktualEntry,
    mesin: MesinData?,
    peekCount: Int,
    onClick: () -> Unit,
    onExpand: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HistoryPreviewRow(entry = front, mesin = mesin, onClick = onClick)
        if (peekCount > 0) {
            PeekBars(count = peekCount, accent = Cyan500)
            TextButton(onClick = onExpand, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "+$peekCount lainnya",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Cyan500),
                )
            }
        }
    }
}

@Composable
private fun HistorySectionHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    val colors = LocalAppColors.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "chevronRotation",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Riwayat Hari Ini",
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = colors.textPrimary),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "$count",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textFaint),
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Sembunyikan" else "Tampilkan",
                tint = colors.textMuted,
                modifier = Modifier.size(18.dp).rotate(rotation),
            )
        }
    }
}

@Composable
private fun HistoryPreviewRow(entry: AktualEntry, mesin: MesinData?, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val corak = entry.corakOverride ?: mesin?.corak ?: "—"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bgElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = entry.mcNo,
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black, color = Cyan500, letterSpacing = (-0.5).sp),
        )
        Text(
            text = corak,
            style = TextStyle(fontSize = 12.sp, color = colors.textMuted),
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Text(
            text = entry.ket,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
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
            text = "Belum ada mesin yang dipantau",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary),
        )
        Text(
            text = "Masukkan nomor mesin + estimasi di kolom bawah untuk mulai",
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
    val lines = state.aktual.mapIndexed { i, a ->
        val mesin = state.db[a.mcNo]
        val corak = a.corakOverride ?: mesin?.corak ?: "—"
        val suffix = if (a.customYard != null) " [${a.customYard}y]" else ""
        "${i + 1}. Mc${a.mcNo} - $corak$suffix - ${a.ket}"
    }
    val text = "Adoel V5\n$dateStr\n\n${lines.joinToString("\n")}\n\nTotal: ${state.aktual.size} doff"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan riwayat"))
}
