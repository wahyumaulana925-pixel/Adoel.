package com.jekael.adoel.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

private enum class SettingsTab { EDIT, LIST, DATA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDrawer(
    state: DoffState,
    onClose: () -> Unit,
    onSetMesin: (String, MesinData) -> Unit,
    onResetMesin: (String) -> Unit,
    onResetDb: () -> Unit,
    showToast: (String) -> Unit,
    showConfirm: (String, () -> Unit) -> Unit,
) {
    var tab by remember { mutableStateOf(SettingsTab.EDIT) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Zinc950,
        sheetMaxWidth = 560.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Zinc700),
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
                Text("Pengaturan", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Zinc100))
                IconButton(onClick = onClose) {
                    Text("✕", style = TextStyle(fontSize = 16.sp, color = Zinc500))
                }
            }

            // Tab switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .background(Zinc900, RoundedCornerShape(12.dp))
                    .padding(4.dp),
            ) {
                listOf(SettingsTab.EDIT to "Edit Mesin", SettingsTab.LIST to "Daftar", SettingsTab.DATA to "Data")
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
                                    color = if (selected) Zinc950 else Zinc400,
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
                    SettingsTab.EDIT -> EditMesinTab(state, onSetMesin, onResetMesin, showToast)
                    SettingsTab.LIST -> DaftarMesinTab(state)
                    SettingsTab.DATA -> DataTab(state, onResetDb, showToast, showConfirm)
                }
            }
        }
    }
}

@Composable
private fun EditMesinTab(
    state: DoffState,
    onSetMesin: (String, MesinData) -> Unit,
    onResetMesin: (String) -> Unit,
    showToast: (String) -> Unit,
) {
    var mcNoInput by remember { mutableStateOf("") }
    var form by remember { mutableStateOf<MesinData?>(null) }
    var loaded by remember { mutableStateOf(false) }

    fun doLoad() {
        val n = mcNoInput.trim()
        if (!n.matches(Regex("^\\d{1,3}$"))) { showToast("Nomor mesin tidak valid"); return }
        val mesin = state.db[n]
        if (mesin == null) { showToast("Mc $n tidak ditemukan"); return }
        form = mesin.copy()
        loaded = true
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = mcNoInput,
                onValueChange = { mcNoInput = it; loaded = false },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nomor mesin", color = Zinc600) },
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = Zinc100, fontSize = 14.sp),
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
                textStyle = TextStyle(color = Zinc100, fontSize = 14.sp),
                singleLine = true,
            )

            FieldLabel("Target Yard")
            OutlinedTextField(
                value = f.targetYard?.toString() ?: "",
                onValueChange = { form = f.copy(targetYard = it.toDoubleOrNull()) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("opsional", color = Zinc600) },
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = Zinc100, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            if (f.tipe == MesinTipe.D405) {
                FieldLabel("Speed (yard/menit)")
                OutlinedTextField(
                    value = f.speed?.toString() ?: "",
                    onValueChange = { form = f.copy(speed = it.toDoubleOrNull()) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("cth: 0.158", color = Zinc600) },
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Zinc100, fontSize = 14.sp),
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
                    placeholder = { Text("cth: 18", color = Zinc600) },
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Zinc100, fontSize = 14.sp),
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
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Zinc400),
                    border = BorderStroke(1.dp, Zinc700),
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

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ChipBtn(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Teal600 else Color.Transparent)
            .border(1.dp, if (selected) Teal500 else Zinc700, shape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Zinc100 else Zinc400,
            ),
        )
    }
}

@Composable
private fun DaftarMesinTab(state: DoffState) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<MesinTipe?>(null) }

    val entries = remember(state.db, search, filter) {
        state.db.entries
            .filter { (k, v) ->
                if (filter != null && v.tipe != filter) return@filter false
                if (search.isNotEmpty() && !k.contains(search) && !v.corak.contains(search, ignoreCase = true)) return@filter false
                v.corak.isNotEmpty() && v.corak != "-"
            }
            .sortedBy { (k, _) -> k.toIntOrNull() ?: 0 }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari nomor / corak...", color = Zinc600) },
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(color = Zinc100, fontSize = 14.sp),
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChipBtn("Semua", filter == null) { filter = null }
            MesinTipe.entries.forEach { t ->
                ChipBtn(t.name, filter == t) { filter = t }
            }
        }
        LazyColumn(
            modifier = Modifier.heightIn(max = 280.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(entries, key = { it.key }) { (k, v) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Zinc800.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(k, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Zinc100), modifier = Modifier.width(32.dp))
                    Text(v.tipe.name, style = TextStyle(fontSize = 11.sp, letterSpacing = 1.sp, color = Zinc500), modifier = Modifier.width(56.dp))
                    Text(v.corak, style = TextStyle(fontSize = 14.sp, color = Zinc300), modifier = Modifier.weight(1f), maxLines = 1)
                    if (v.targetYard != null) Text("${v.targetYard}y", style = TextStyle(fontSize = 11.sp, color = Zinc600))
                }
            }
            if (entries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Text("Tidak ditemukan", color = Zinc600, style = TextStyle(fontSize = 14.sp))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DataTab(
    state: DoffState,
    onResetDb: () -> Unit,
    showToast: (String) -> Unit,
    showConfirm: (String, () -> Unit) -> Unit,
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Zinc300),
            border = BorderStroke(1.dp, Zinc700),
        ) { Text("Export / Bagikan Data") }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = Zinc800)
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
