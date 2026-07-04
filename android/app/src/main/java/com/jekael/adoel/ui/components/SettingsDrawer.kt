package com.jekael.adoel.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.BuildConfig
import com.jekael.adoel.data.*
import com.jekael.adoel.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class SettingsTab { MESIN, DATA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDrawer(
    state: DoffState,
    onClose: () -> Unit,
    onSetMesin: (String, MesinData) -> Unit,
    onResetMesin: (String) -> Unit,
    onResetDb: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onExportJson: () -> String,
    onImport: (String) -> Unit,
    showToast: (String) -> Unit,
    showConfirm: (String, () -> Unit) -> Unit,
) {
    var tab by remember { mutableStateOf(SettingsTab.MESIN) }
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()

    // Panel slides in from the right (where the menu icon lives) and slides back out on close,
    // even though the caller mounts/unmounts this composable abruptly via `settingsOpen`.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    fun requestClose() {
        visible = false
    }

    LaunchedEffect(visible) {
        if (!visible) {
            delay(220)
            onClose()
        }
    }

    // Manual drag offset for swipe-right-to-dismiss; separate from the AnimatedVisibility
    // enter/exit transition above so the two animation systems never fight over the same value.
    val dragOffset = remember { Animatable(0f) }
    var panelWidthPx by remember { mutableStateOf(0f) }

    BackHandler(enabled = visible) { requestClose() }

    // Rendered directly in the caller's own window (no Dialog) so our AnimatedVisibility is the
    // ONLY thing driving the enter/exit motion — a Dialog's own window has a default scale/fade
    // animation that fires before any in-content effect can suppress it, which showed up as a
    // diagonal entrance no matter what.
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(260)),
        exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(220)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
                .onGloballyPositioned { panelWidthPx = it.size.width.toFloat() }
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val width = if (panelWidthPx > 0f) panelWidthPx else 1f
                            if (dragOffset.value > width * 0.3f) {
                                scope.launch {
                                    dragOffset.animateTo(width, animationSpec = tween(200))
                                    onClose()
                                }
                            } else {
                                scope.launch {
                                    dragOffset.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { dragOffset.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        val newVal = (dragOffset.value + dragAmount).coerceAtLeast(0f)
                        scope.launch { dragOffset.snapTo(newVal) }
                    }
                },
            // No systemBarsPadding() here — this panel now lives inside MainScreen's own root
            // Box, which already insets its children from the system bars once.
        ) {
            // Header + tab switcher — floating card, matching the header/console bar's look
            // (shadow + rounded corners lifted off the screen background) instead of sitting
            // flat directly on colors.bg like the rest of this full-bleed panel.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.bgElevated),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Pengaturan", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary))
                        IconButton(onClick = { requestClose() }) {
                            CloseIcon()
                        }
                    }
                    SlidingToggle(
                        labelLeft = "Mesin",
                        labelRight = "Data",
                        selectedIndex = if (tab == SettingsTab.MESIN) 0 else 1,
                        onSelect = { tab = if (it == 0) SettingsTab.MESIN else SettingsTab.DATA },
                        containerColor = colors.bgElevated2,
                        activeColorLeft = Teal500,
                        activeColorRight = Teal500,
                        activeTextColorLeft = Zinc950,
                        activeTextColorRight = Zinc950,
                        inactiveTextColor = colors.textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 14.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = tab,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(animationSpec = tween(220)) { w -> dir * w } + fadeIn(tween(180)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(220)) { w -> -dir * w } + fadeOut(tween(140)))
                },
                label = "settingsTabContent",
            ) { t ->
                when (t) {
                    SettingsTab.MESIN -> MesinTab(state, onSetMesin, onResetMesin, showToast, showConfirm)
                    SettingsTab.DATA -> DataTab(state, onResetDb, onSetThemeMode, onExportJson, onImport, showToast, showConfirm)
                }
            }
        }
    }
}

@Composable
private fun MesinTab(
    state: DoffState,
    onSetMesin: (String, MesinData) -> Unit,
    onResetMesin: (String) -> Unit,
    showToast: (String) -> Unit,
    showConfirm: (String, () -> Unit) -> Unit,
) {
    val colors = LocalAppColors.current
    var activeMcNo by remember { mutableStateOf<String?>(null) }
    var form by remember { mutableStateOf<MesinData?>(null) }
    // Whether this machine already had data configured when the panel was opened — captured
    // once at open time so it doesn't flicker as the user types into a blank entry. Reset only
    // makes sense (and is only shown) when there's actually saved data to revert.
    var hadExistingData by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }

    fun loadFrom(mcNo: String, mesin: MesinData) {
        activeMcNo = mcNo
        form = mesin.copy()
        hadExistingData = mesin.corak.isNotEmpty() && mesin.corak != "-"
    }

    val entries = remember(state.db, search, showAll) {
        state.db.entries
            .filter { (k, v) ->
                if (!showAll && (v.corak.isEmpty() || v.corak == "-")) return@filter false
                if (search.isNotEmpty() && !k.contains(search) && !v.corak.contains(search, ignoreCase = true)) return@filter false
                true
            }
            .sortedBy { (k, _) -> k.toIntOrNull() ?: 0 }
    }

    // A search that's exactly a bare mc number not yet configured (corak masih "-")
    // gets offered as "configure this new machine" instead of showing up empty-handed.
    val unconfigured = remember(state.db, search) {
        val n = search.trim()
        if (n.matches(Regex("^\\d{1,3}$"))) {
            state.db[n]?.let { mesin -> if (mesin.corak.isEmpty() || mesin.corak == "-") n to mesin else null }
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari nomor / corak, atau ketik nomor baru", color = colors.textFaint) },
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { unconfigured?.let { (n, m) -> loadFrom(n, m) } }),
            singleLine = true,
        )
        Row(
            modifier = Modifier
                .clickable { showAll = !showAll }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = showAll,
                onCheckedChange = { showAll = it },
                colors = CheckboxDefaults.colors(checkedColor = Teal500, uncheckedColor = colors.border),
            )
            Text(
                "Tampilkan semua (termasuk corak \"-\")",
                style = TextStyle(fontSize = 13.sp, color = colors.textSecondary),
            )
        }

        if (unconfigured != null) {
            val (n, m) = unconfigured
            OutlinedButton(
                onClick = { loadFrom(n, m) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal500),
                border = BorderStroke(1.dp, Teal500),
            ) { Text("Konfigurasi Mc $n (belum diatur)") }
        }

        HorizontalDivider(color = colors.border)

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.forEach { (k, v) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 3.dp, shape = RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.bgElevated2)
                        .clickable { loadFrom(k, v) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(k, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary), modifier = Modifier.width(32.dp))
                    Text(v.tipe.name, style = TextStyle(fontSize = 11.sp, letterSpacing = 1.sp, color = colors.textMuted), modifier = Modifier.width(56.dp))
                    Text(
                        v.corak,
                        style = TextStyle(fontSize = 14.sp, color = colors.textPrimary),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (v.targetYard != null) Text("${formatYard(v.targetYard)}y", style = TextStyle(fontSize = 11.sp, color = colors.textFaint))
                }
            }
            if (entries.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("Tidak ditemukan", color = colors.textFaint, style = TextStyle(fontSize = 14.sp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    val mcNo = activeMcNo
    val f = form
    if (mcNo != null && f != null) {
        MesinEditPanel(
            mcNo = mcNo,
            form = f,
            showReset = hadExistingData,
            showToast = showToast,
            onFormChange = { form = it },
            onClose = { activeMcNo = null; form = null },
            onCancel = { activeMcNo = null; form = null },
            onReset = {
                showConfirm("Reset Mc $mcNo ke default? Corak, target yard, dan pengaturan lain akan dihapus.") {
                    onResetMesin(mcNo)
                    showToast("Mc $mcNo direset ke default")
                    activeMcNo = null; form = null
                }
            },
            onSave = {
                val corak = f.corak.trim().ifEmpty { "-" }
                onSetMesin(mcNo, f.copy(corak = corak))
                showToast("Mc $mcNo disimpan ✓")
                activeMcNo = null; form = null; search = ""
            },
        )
    }
}

@Composable
private fun MesinEditPanel(
    mcNo: String,
    form: MesinData,
    showReset: Boolean,
    showToast: (String) -> Unit,
    onFormChange: (MesinData) -> Unit,
    onClose: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = LocalAppColors.current
    val f = form
    // Kept as raw text (not re-derived from the Double each recomposition) so a whole-number
    // value like 303 doesn't force an unwanted ".0" suffix, and mid-typing text like "303." isn't
    // reformatted out from under the user before they finish entering a decimal.
    var targetYardText by remember(mcNo) { mutableStateOf(f.targetYard?.let { formatYard(it) } ?: "") }
    var speedText by remember(mcNo) { mutableStateOf(f.speed?.let { formatYard(it) } ?: "") }
    var koreksiText by remember(mcNo) { mutableStateOf(f.koreksi?.let { formatYard(it) } ?: "") }

    FloatingEditDialog(onDismissRequest = onClose) {
        Text(
            text = "Mc $mcNo",
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, color = colors.textPrimary),
        )

        Spacer(Modifier.height(16.dp))

        FieldLabel("Tipe Mesin")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MesinTipe.entries.forEach { t ->
                ChipBtn(t.name, f.tipe == t) { onFormChange(f.copy(tipe = t)) }
            }
        }

        Spacer(Modifier.height(20.dp))

        FieldLabel("Corak")
        OutlinedTextField(
            value = f.corak,
            onValueChange = { onFormChange(f.copy(corak = it)) },
            modifier = Modifier.fillMaxWidth(),
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
            singleLine = true,
        )

        Spacer(Modifier.height(16.dp))

        FieldLabel("Target Yard")
        OutlinedTextField(
            value = targetYardText,
            onValueChange = {
                targetYardText = it
                onFormChange(f.copy(targetYard = it.replace(',', '.').toDoubleOrNull()))
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("opsional", color = colors.textFaint) },
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )

        if (f.tipe == MesinTipe.D405) {
            Spacer(Modifier.height(16.dp))
            FieldLabel("Speed (yard/menit)")
            OutlinedTextField(
                value = speedText,
                onValueChange = {
                    speedText = it
                    onFormChange(f.copy(speed = it.replace(',', '.').toDoubleOrNull()))
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("cth: 0.158", color = colors.textFaint) },
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
        }

        if (f.tipe == MesinTipe.D408) {
            Spacer(Modifier.height(16.dp))
            FieldLabel("Koreksi (menit)")
            OutlinedTextField(
                value = koreksiText,
                onValueChange = {
                    koreksiText = it
                    onFormChange(f.copy(koreksi = it.replace(',', '.').toDoubleOrNull()))
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("cth: 18", color = colors.textFaint) },
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                border = BorderStroke(1.dp, colors.border),
            ) { Text("Batal") }
            if (showReset) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
                    border = BorderStroke(1.dp, Red400),
                ) { Text("Reset") }
            }
            Button(
                onClick = {
                    val targetYardInvalid = targetYardText.isNotBlank() && f.targetYard == null
                    val speedInvalid = f.tipe == MesinTipe.D405 && speedText.isNotBlank() && f.speed == null
                    val koreksiInvalid = f.tipe == MesinTipe.D408 && koreksiText.isNotBlank() && f.koreksi == null
                    when {
                        targetYardInvalid -> showToast("Target Yard tidak valid, cek kembali")
                        speedInvalid -> showToast("Speed tidak valid, cek kembali")
                        koreksiInvalid -> showToast("Koreksi tidak valid, cek kembali")
                        else -> onSave()
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal500),
            ) { Text("Simpan", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun ChipBtn(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Teal600 else Color.Transparent)
            .border(1.dp, if (selected) Teal500 else colors.border, shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Zinc100 else colors.textSecondary,
            ),
        )
    }
}

@Composable
private fun DataTab(
    state: DoffState,
    onResetDb: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onExportJson: () -> String,
    onImport: (String) -> Unit,
    showToast: (String) -> Unit,
    showConfirm: (String, () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val currentTheme = remember(state.themeMode) {
        runCatching { ThemeMode.valueOf(state.themeMode) }.getOrDefault(ThemeMode.SYSTEM)
    }

    // Backup: user picks where to save a .json file; we write the full-state JSON to it.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(onExportJson().toByteArray()) }
                }.isSuccess
            }
            showToast(if (ok) "Data dicadangkan ✓" else "⚠ Gagal menyimpan file")
        }
    }

    // Restore: user picks a backup file; we read its text and hand it to onImport (which confirms).
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (text != null) onImport(text) else showToast("⚠ Gagal membaca file")
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FieldLabel("Tema Aplikasi")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipBtn("Sistem", currentTheme == ThemeMode.SYSTEM) { onSetThemeMode(ThemeMode.SYSTEM) }
            ChipBtn("Gelap", currentTheme == ThemeMode.DARK) { onSetThemeMode(ThemeMode.DARK) }
            ChipBtn("Terang", currentTheme == ThemeMode.LIGHT) { onSetThemeMode(ThemeMode.LIGHT) }
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = colors.border)
        Spacer(Modifier.height(4.dp))

        FieldLabel("Cadangan Data")
        Text(
            "Cadangkan seluruh data (mesin, estimasi, riwayat doff, tema) ke file, atau pulihkan dari file cadangan.",
            style = TextStyle(fontSize = 12.sp, color = colors.textMuted),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                    runCatching { exportLauncher.launch("adoel-backup-$stamp.json") }
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                border = BorderStroke(1.dp, colors.border),
            ) { Text("Cadangkan") }
            OutlinedButton(
                onClick = {
                    runCatching { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal500),
                border = BorderStroke(1.dp, Teal500),
            ) { Text("Pulihkan") }
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = colors.border)
        Spacer(Modifier.height(4.dp))

        OutlinedButton(
            onClick = {
                showConfirm("Reset semua data ke default? Estimasi & riwayat akan hilang.") {
                    onResetDb()
                    showToast("Data direset ke default")
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
            border = BorderStroke(1.dp, Red700.copy(alpha = 0.5f)),
        ) { Text("Reset ke Default") }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = colors.border)
        Spacer(Modifier.height(4.dp))

        var aboutOpen by remember { mutableStateOf(false) }
        var helpOpen by remember { mutableStateOf(false) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { helpOpen = true },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                border = BorderStroke(1.dp, colors.border),
            ) { Text("Bantuan") }
            OutlinedButton(
                onClick = { aboutOpen = true },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                border = BorderStroke(1.dp, colors.border),
            ) { Text("Tentang") }
        }
        if (aboutOpen) {
            AboutDialog(onClose = { aboutOpen = false })
        }
        if (helpOpen) {
            OnboardingDialog(onClose = { helpOpen = false })
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AboutDialog(onClose: () -> Unit) {
    val colors = LocalAppColors.current
    FloatingEditDialog(onDismissRequest = onClose) {
        Text("Tentang", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary))
        Spacer(Modifier.height(16.dp))
        Text("Adoel.", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black, color = colors.textPrimary))
        Spacer(Modifier.height(4.dp))
        Text(
            "Versi ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
            style = TextStyle(fontSize = 13.sp, color = colors.textSecondary),
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal500),
        ) { Text("Tutup", fontWeight = FontWeight.SemiBold) }
    }
}
