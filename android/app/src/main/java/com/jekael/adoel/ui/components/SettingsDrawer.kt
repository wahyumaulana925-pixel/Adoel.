package com.jekael.adoel.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.GsonBuilder
import com.jekael.adoel.data.*
import com.jekael.adoel.ui.theme.*

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
    showToast: (String) -> Unit,
    showConfirm: (String, () -> Unit) -> Unit,
) {
    var tab by remember { mutableStateOf(SettingsTab.MESIN) }
    val colors = LocalAppColors.current

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = colors.bg,
        sheetMaxWidth = 560.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.border),
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Pengaturan", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary))
                IconButton(onClick = onClose) {
                    Text("✕", style = TextStyle(fontSize = 16.sp, color = colors.textMuted))
                }
            }

            // Tab switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .background(colors.bgElevated2, RoundedCornerShape(12.dp))
                    .padding(4.dp),
            ) {
                listOf(SettingsTab.MESIN to "Mesin", SettingsTab.DATA to "Data")
                    .forEach { (t, label) ->
                        val selected = tab == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Teal500 else Color.Transparent)
                                .clickable { tab = t }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) Zinc950 else colors.textSecondary,
                                ),
                            )
                        }
                    }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
            ) {
                when (tab) {
                    SettingsTab.MESIN -> MesinTab(state, onSetMesin, onResetMesin, showToast)
                    SettingsTab.DATA -> DataTab(state, onResetDb, onSetThemeMode, showToast, showConfirm)
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
) {
    val colors = LocalAppColors.current
    var mcNoInput by remember { mutableStateOf("") }
    var form by remember { mutableStateOf<MesinData?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<MesinTipe?>(null) }

    fun doLoad() {
        val n = mcNoInput.trim()
        if (!n.matches(Regex("^\\d{1,3}$"))) { showToast("Nomor mesin tidak valid"); return }
        val mesin = state.db[n]
        if (mesin == null) { showToast("Mc $n tidak ditemukan"); return }
        form = mesin.copy()
        loaded = true
    }

    fun loadFrom(mcNo: String, mesin: MesinData) {
        mcNoInput = mcNo
        form = mesin.copy()
        loaded = true
    }

    val entries = remember(state.db, search, filter) {
        state.db.entries
            .filter { (k, v) ->
                if (filter != null && v.tipe != filter) return@filter false
                if (search.isNotEmpty() && !k.contains(search) && !v.corak.contains(search, ignoreCase = true)) return@filter false
                v.corak.isNotEmpty() && v.corak != "-"
            }
            .sortedBy { (k, _) -> k.toIntOrNull() ?: 0 }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = mcNoInput,
                onValueChange = { mcNoInput = it; loaded = false },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nomor mesin", color = colors.textFaint) },
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { doLoad() }),
                singleLine = true,
            )
            Button(
                onClick = { doLoad() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal600),
            ) { Text("Load", fontWeight = FontWeight.SemiBold) }
        }

        if (loaded && form != null) {
            val f = form!!

            FieldLabel("Tipe")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MesinTipe.entries.forEach { tipe ->
                    ChipBtn(
                        label = tipe.name,
                        selected = f.tipe == tipe,
                        onClick = {
                            form = f.copy(
                                tipe = tipe,
                                speed = if (tipe == MesinTipe.D405) f.speed else null,
                                koreksi = if (tipe == MesinTipe.D408) f.koreksi else null,
                            )
                        },
                    )
                }
            }

            FieldLabel("Corak")
            OutlinedTextField(
                value = f.corak,
                onValueChange = { form = f.copy(corak = it) },
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                singleLine = true,
            )

            FieldLabel("Target Yard")
            OutlinedTextField(
                value = f.targetYard?.toString() ?: "",
                onValueChange = { form = f.copy(targetYard = it.toDoubleOrNull()) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("opsional", color = colors.textFaint) },
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            if (f.tipe == MesinTipe.D405) {
                FieldLabel("Speed (yard/menit)")
                OutlinedTextField(
                    value = f.speed?.toString() ?: "",
                    onValueChange = { form = f.copy(speed = it.toDoubleOrNull()) },
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
                FieldLabel("Koreksi (menit)")
                OutlinedTextField(
                    value = f.koreksi?.toString() ?: "",
                    onValueChange = { form = f.copy(koreksi = it.toDoubleOrNull()) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("cth: 18", color = colors.textFaint) },
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onResetMesin(mcNoInput.trim())
                        showToast("Mc $mcNoInput direset ke default")
                        loaded = false; form = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                    border = BorderStroke(1.dp, colors.border),
                ) { Text("Reset") }
                Button(
                    onClick = {
                        val corak = f.corak.trim().ifEmpty { "-" }
                        onSetMesin(mcNoInput.trim(), f.copy(corak = corak))
                        showToast("Mc $mcNoInput disimpan ✓")
                        loaded = false; form = null; mcNoInput = ""
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                ) { Text("Simpan", fontWeight = FontWeight.SemiBold) }
            }
        }

        HorizontalDivider(color = colors.border)

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari nomor / corak...", color = colors.textFaint) },
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChipBtn("Semua", filter == null) { filter = null }
            MesinTipe.entries.forEach { t ->
                ChipBtn(t.name, filter == t) { filter = t }
            }
        }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            entries.forEach { (k, v) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.bgElevated2.copy(alpha = 0.5f))
                        .clickable { loadFrom(k, v) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(k, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary), modifier = Modifier.width(32.dp))
                    Text(v.tipe.name, style = TextStyle(fontSize = 11.sp, letterSpacing = 1.sp, color = colors.textMuted), modifier = Modifier.width(56.dp))
                    Text(v.corak, style = TextStyle(fontSize = 14.sp, color = colors.textPrimary), modifier = Modifier.weight(1f), maxLines = 1)
                    if (v.targetYard != null) Text("${v.targetYard}y", style = TextStyle(fontSize = 11.sp, color = colors.textFaint))
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
    showToast: (String) -> Unit,
    showConfirm: (String, () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val currentTheme = remember(state.themeMode) {
        runCatching { ThemeMode.valueOf(state.themeMode) }.getOrDefault(ThemeMode.SYSTEM)
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

        OutlinedButton(
            onClick = {
                val gson = GsonBuilder().create()
                val data = gson.toJson(mapOf(
                    "db" to state.db.mapValues { (_, v) ->
                        mapOf("tipe" to v.tipe.name, "corak" to v.corak, "targetYard" to v.targetYard, "speed" to v.speed, "koreksi" to v.koreksi)
                    },
                    "estimasiCount" to state.estimasi.size,
                    "aktualCount" to state.aktual.size,
                ))
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, data)
                }
                context.startActivity(Intent.createChooser(intent, "Export data"))
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
            border = BorderStroke(1.dp, colors.border),
        ) { Text("Export / Bagikan Data") }

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

        Spacer(Modifier.height(8.dp))
    }
}
