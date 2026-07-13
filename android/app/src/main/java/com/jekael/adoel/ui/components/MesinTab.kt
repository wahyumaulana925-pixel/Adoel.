package com.jekael.adoel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.*
import com.jekael.adoel.ui.theme.*

@Composable
internal fun MesinTab(
    state: DoffState,
    headerHeight: Dp,
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
    // Grouped per MesinTipe (fixed order, same as the physical floor's layout-by-machine-type
    // sheet) with a header per group — same icon/color language as RadarCard (Batch 1) so
    // "what kind of machine is this" reads the same across Radar, Pengaturan, and Statistik.
    val groupedEntries = remember(entries) {
        val order = listOf(MesinTipe.TAPPET, MesinTipe.CAM, MesinTipe.D405, MesinTipe.D408)
        val byTipe = entries.groupBy { (_, v) -> v.tipe }
        order.mapNotNull { tipe -> byTipe[tipe]?.let { tipe to it } }
    }

    // A search that's exactly a bare mc number not yet configured (corak masih "-")
    // gets offered as "configure this new machine" instead of showing up empty-handed.
    val unconfigured = remember(state.db, search) {
        val n = search.trim()
        if (n.matches(Regex("^\\d{1,3}$"))) {
            state.db[n]?.let { mesin -> if (mesin.corak.isEmpty() || mesin.corak == "-") n to mesin else null }
        } else null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Scrolls behind the floating header/tab-switcher card above instead of being pushed
        // down by it — see the Box-overlay comment on SettingsDrawer's root.
        item(key = "top_spacer") { Spacer(Modifier.height(10.dp + headerHeight + 16.dp)) }
        item(key = "search") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari nomor / corak, atau ketik nomor baru", color = colors.textFaint) },
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = AppType.FieldText.copy(color = colors.textPrimary),
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
                        colors = CheckboxDefaults.colors(checkedColor = Cyan500, uncheckedColor = colors.border),
                    )
                    Text(
                        "Tampilkan semua (termasuk corak \"-\")",
                        style = AppType.BodySmall.copy(color = colors.textSecondary),
                    )
                }

                if (unconfigured != null) {
                    val (n, m) = unconfigured
                    OutlinedButton(
                        onClick = { loadFrom(n, m) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan500),
                        border = BorderStroke(1.dp, Cyan500),
                    ) { Text("Konfigurasi Mc $n (belum diatur)") }
                }

                HorizontalDivider(color = colors.border)
            }
        }

        if (groupedEntries.isEmpty()) {
            item(key = "empty") {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("Tidak ditemukan", color = colors.textFaint, style = AppType.FieldText)
                }
            }
        }
        groupedEntries.forEach { (tipe, rows) ->
            item(key = "head_${tipe.name}") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bg)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = mesinTipeIcon(tipe),
                        contentDescription = null,
                        tint = mesinTipeColor(tipe),
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = tipe.name,
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = mesinTipeColor(tipe)),
                    )
                    Text(
                        text = "${rows.size}",
                        style = AppType.Caption.copy(color = colors.textFaint),
                    )
                }
            }
            items(rows, key = { (k, _) -> k }) { (k, v) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .elevatedListCard(elevation = 5.dp, backgroundColor = colors.bgElevated2)
                        .clickable { loadFrom(k, v) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(k, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary), modifier = Modifier.width(32.dp))
                    Text(
                        v.corak,
                        style = AppType.FieldText.copy(color = colors.textPrimary),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (v.targetYard != null) Text("${formatYard(v.targetYard)}y", style = TextStyle(fontSize = 11.sp, color = colors.textFaint))
                }
            }
        }
        item(key = "bottom_spacer") { Spacer(Modifier.height(8.dp)) }
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
