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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
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
    var editEstMc by remember { mutableStateOf<String?>(null) }
    var editAktId by remember { mutableStateOf<Int?>(null) }
    var historyExpanded by remember { mutableStateOf(false) }

    val inputFocus = remember { FocusRequester() }

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

    val doffCount = state.aktual.size
    val totalMc = remember(state.estimasi, state.aktual) {
        (state.estimasi.keys + state.aktual.map { it.mcNo }).toSet().size
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Zinc950)
            .systemBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Zinc950)
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Branding
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Adoel",
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Black, color = Zinc100, letterSpacing = (-0.5).sp),
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
                        .background(Zinc900, CircleShape),
                ) {
                    GearIcon()
                }
            }

            HorizontalDivider(color = Zinc800.copy(alpha = 0.6f))

            // Shift progress — persistent framing of how far along the current shift is
            if (totalMc > 0) {
                val shiftFraction = doffCount.toFloat() / totalMc
                val animatedFraction by animateFloatAsState(
                    targetValue = shiftFraction.coerceIn(0f, 1f),
                    animationSpec = tween(400),
                    label = "shiftProgress",
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Zinc950)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Progres shift",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, color = Zinc500),
                        )
                        Text(
                            text = "$doffCount dari $totalMc selesai",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Cyan400),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Zinc800),
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
                HorizontalDivider(color = Zinc800.copy(alpha = 0.6f))
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
                        containerColor = Color(0xFF292007),
                        contentColor = Amber400,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("Notifikasi nonaktif — ketuk untuk izinkan", style = TextStyle(fontSize = 12.sp))
                }
                HorizontalDivider(color = Amber700.copy(alpha = 0.5f))
            }

            // Main scrollable content
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    SectionHeader(title = "Mesin Siap", count = radarList.size)
                    if (radarList.isNotEmpty()) {
                        Text(
                            text = "Tahan kartu untuk edit estimasi",
                            style = TextStyle(fontSize = 11.sp, color = Zinc600),
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
                        )
                    }
                }

                if (radarList.isEmpty()) {
                    item {
                        EmptyState(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp))
                    }
                } else {
                    if (segeraList.isNotEmpty()) {
                        item {
                            UrgencyBandHeader(label = "Segera", color = Red400)
                        }
                        items(segeraList, key = { it.mcNo }) { est ->
                            RadarCard(
                                est = est,
                                mesin = state.db[est.mcNo],
                                nowAbs = nowAbs,
                                onDoff = { handleDoff(est.mcNo) },
                                onHapus = { handleHapusEst(est.mcNo) },
                                onLongPress = { editEstMc = est.mcNo },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    if (menungguList.isNotEmpty()) {
                        item {
                            UrgencyBandHeader(label = "Menunggu", color = Cyan400)
                        }
                        items(menungguList, key = { it.mcNo }) { est ->
                            RadarCard(
                                est = est,
                                mesin = state.db[est.mcNo],
                                nowAbs = nowAbs,
                                onDoff = { handleDoff(est.mcNo) },
                                onHapus = { handleHapusEst(est.mcNo) },
                                onLongPress = { editEstMc = est.mcNo },
                                modifier = Modifier.animateItem(),
                            )
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

                if (historyExpanded) {
                    if (recentHistory.isEmpty()) {
                        item {
                            Text(
                                text = "Belum ada doff hari ini",
                                style = TextStyle(fontSize = 13.sp, color = Zinc600),
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    } else {
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
                    }
                }
            }

            HorizontalDivider(color = Zinc800.copy(alpha = 0.6f))

            // Console command bar — the sole input model for estimasi/doffing
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Zinc950)
                    .padding(horizontal = 12.dp)
                    .padding(top = 10.dp)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = 4.dp),
            ) {
                // Mode toggle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Zinc800, RoundedCornerShape(50.dp))
                        .padding(4.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { mode = Mode.ESTIMASI },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mode == Mode.ESTIMASI) Amber500 else Color.Transparent,
                                contentColor = if (mode == Mode.ESTIMASI) Zinc950 else Zinc500,
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
                                contentColor = if (mode == Mode.AKTUAL) Zinc100 else Zinc500,
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
                                color = Zinc600,
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan500,
                            unfocusedBorderColor = Zinc700,
                            cursorColor = Cyan500,
                            focusedContainerColor = Zinc800,
                            unfocusedContainerColor = Zinc800,
                        ),
                        shape = RoundedCornerShape(50.dp),
                        textStyle = TextStyle(
                            color = Zinc100,
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
                        colors = ButtonDefaults.buttonColors(containerColor = Zinc800, contentColor = Zinc400),
                        contentPadding = PaddingValues(0.dp),
                    ) { HistoryIcon() }
                }
            }
        }

        // Toast at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
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
            onHapus = { entry ->
                uiVm.showConfirm("Hapus doff Mc ${entry.mcNo}?") {
                    doffVm.hapusAktualById(entry.id)
                    uiVm.showToast("Mc ${entry.mcNo} dihapus", undo = { doffVm.restoreAktual(entry) })
                }
            },
            onShare = { shareHistory(context, state) },
            onFinishShift = {
                uiVm.showConfirm("Akhiri shift? Riwayat ${state.aktual.size} doff akan dihapus.") {
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
            onImportDb = { db -> db.forEach { (k, v) -> doffVm.setMesin(k, v) } },
            showToast = { uiVm.showToast(it) },
            showConfirm = { msg, fn -> uiVm.showConfirm(msg, onConfirm = fn) },
        )
    }

    if (editEstMc != null) {
        EditEstSheet(
            mcNo = editEstMc,
            state = state,
            nowAbs = nowAbs,
            onClose = { editEstMc = null },
            onSave = { rawInput, corakOverride ->
                val mcNo = editEstMc!!
                if (rawInput.isNotBlank()) {
                    val result = doffVm.prosesBarisKondisiMesin("$mcNo ${rawInput.trim().uppercase()}", nowAbsMin())
                    if (result is ProsesResult.Err) { uiVm.showToast("⚠ ${result.msg}"); return@EditEstSheet }
                    if (result is ProsesResult.Ok && result.estAbs != null) {
                        NotificationHelper.scheduleNotif(context, mcNo, result.estAbs)
                    }
                }
                val cleanCorak = corakOverride?.trim()?.takeIf { it.isNotEmpty() }
                val mesin = state.db[mcNo]
                doffVm.updateEstimasi(
                    mcNo,
                    corakOverride = if (cleanCorak != mesin?.corak) cleanCorak else null,
                    yardOverride = null,
                )
                state.estimasi[mcNo]?.let { NotificationHelper.scheduleNotif(context, mcNo, it.estAbsMin) }
                uiVm.showToast("Mc $mcNo diperbarui")
                editEstMc = null
            },
            onHapus = {
                val mcNo = editEstMc!!
                val prevEst = state.estimasi[mcNo]
                doffVm.hapusEstimasi(mcNo)
                NotificationHelper.cancelNotif(context, mcNo)
                uiVm.showToast("Mc $mcNo dihapus", undo = {
                    if (prevEst != null) {
                        doffVm.restoreEstimasi(prevEst)
                        NotificationHelper.scheduleNotif(context, prevEst.mcNo, prevEst.estAbsMin)
                    }
                })
                editEstMc = null
            },
        )
    }

    if (editAktId != null) {
        EditAktSheet(
            aktualId = editAktId,
            state = state,
            onClose = { editAktId = null },
            onSave = { id, ket ->
                doffVm.updateAktual(id, ket)
                uiVm.showToast("Riwayat diperbarui")
                editAktId = null
            },
            onHapus = { entry ->
                uiVm.showConfirm("Hapus doff Mc ${entry.mcNo}?") {
                    doffVm.hapusAktualById(entry.id)
                    uiVm.showToast("Mc ${entry.mcNo} dihapus", undo = { doffVm.restoreAktual(entry) })
                    editAktId = null
                }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = Zinc300),
        )
        if (count > 0) {
            Text(
                text = "$count",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Zinc600),
            )
        }
    }
}

@Composable
private fun UrgencyBandHeader(label: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
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
}

@Composable
private fun HistorySectionHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
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
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = Zinc300),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "$count",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Zinc600),
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Sembunyikan" else "Tampilkan",
                tint = Zinc500,
                modifier = Modifier.size(18.dp).rotate(rotation),
            )
        }
    }
}

@Composable
private fun HistoryPreviewRow(entry: AktualEntry, mesin: MesinData?, onClick: () -> Unit) {
    val corak = entry.corakOverride ?: mesin?.corak ?: "—"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Zinc900)
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
            style = TextStyle(fontSize = 12.sp, color = Zinc500),
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Text(
            text = entry.ket,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Zinc300),
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(Zinc800.copy(alpha = 0.3f)))
            Box(Modifier.size(72.dp).clip(CircleShape).background(Zinc950))
            Box(Modifier.size(72.dp).clip(CircleShape).background(Zinc800.copy(alpha = 0.2f)))
            Box(Modifier.size(48.dp).clip(CircleShape).background(Zinc950))
            Box(Modifier.size(12.dp).clip(CircleShape).background(Zinc700))
        }
        Text(
            text = "Belum ada mesin yang dipantau",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Zinc400),
        )
        Text(
            text = "Masukkan nomor mesin + estimasi di kolom bawah untuk mulai",
            style = TextStyle(fontSize = 12.sp, color = Zinc600, lineHeight = 17.sp),
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
