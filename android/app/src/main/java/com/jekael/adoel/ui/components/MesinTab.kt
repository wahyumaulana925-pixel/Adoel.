package com.jekael.adoel.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Texture
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
    val density = LocalDensity.current
    var activeMcNo by remember { mutableStateOf<String?>(null) }
    var form by remember { mutableStateOf<MesinData?>(null) }
    // Whether this machine already had data configured when the panel was opened — captured
    // once at open time so it doesn't flicker as the user types into a blank entry. Reset only
    // makes sense (and is only shown) when there's actually saved data to revert.
    var hadExistingData by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    var consoleHeight by remember { mutableStateOf(0.dp) }

    fun loadFrom(mcNo: String, mesin: MesinData) {
        activeMcNo = mcNo
        form = mesin.copy()
        hadExistingData = mesin.corak.isNotEmpty() && mesin.corak != "-"
    }

    // Search matches only the mc number (Master Blueprint v9.2 §4) — the bottom console bar
    // that drives [search] is a numeric-only field anyway, so corak/yard were never reachable
    // through it in practice.
    val entries = remember(state.db, search, showAll) {
        state.db.entries
            .filter { (k, v) ->
                if (!showAll && (v.corak.isEmpty() || v.corak == "-")) return@filter false
                if (search.isNotEmpty() && !k.contains(search)) return@filter false
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Scrolls behind the floating header/tab-switcher card above instead of being pushed
            // down by it — see the Box-overlay comment on SettingsDrawer's root.
            item(key = "top_spacer") { Spacer(Modifier.height(10.dp + headerHeight + 16.dp)) }
            item(key = "search_hint") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            shape = RoundedCornerShape(Dimens.RadiusControl),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan500),
                            border = BorderStroke(1.dp, Cyan500),
                        ) { Text("Konfigurasi Mc $n (belum diatur)") }
                    }

                    WovenDivider()
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
                            .fabricTextureSubtle()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MesinTipeIcon(
                            tipe = tipe,
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
                            .elevatedListCard(backgroundColor = colors.bgElevated2)
                            .clickable { loadFrom(k, v) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(k, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary), modifier = Modifier.width(32.dp))
                        // Same kain marker as RadarCard's corak/yard line — this row is Pengaturan >
                        // Mesin's equivalent of that line (corak + target yard), so it gets the same
                        // visual anchor instead of reading as unrelated plain text.
                        Icon(
                            imageVector = Icons.Outlined.Texture,
                            contentDescription = null,
                            tint = colors.textFaint,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            v.corak,
                            style = AppType.FieldText.copy(color = colors.textPrimary),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (v.targetYard != null) Text("${formatYard(v.targetYard)}y", style = TextStyle(fontSize = 12.sp, color = colors.textFaint))
                    }
                }
            }
            item(key = "bottom_spacer") { Spacer(Modifier.height(consoleHeight + 16.dp)) }
        }

        // Top/bottom fade — same soft-edge treatment as MainScreen's list (Master Blueprint
        // v9.2 §10), so rows ease out under SettingsDrawer's header and this tab's own console
        // instead of cutting off sharply.
        EdgeFadeScrim(atTop = true, height = 10.dp + headerHeight + 16.dp)
        EdgeFadeScrim(atTop = false, height = consoleHeight + 16.dp)

        // Floating console bar — search-and-jump for one machine at a time (Master Blueprint
        // v9.2 §8). Deliberately a single numeric field (no comma/space bulk entry like the old
        // "22 33 44" pattern): typing filters the list live via [search], and the edit icon opens
        // that exact machine's editor whether it's already configured or brand new. This replaces
        // the old top search bar entirely — every other DB field still only changes here in
        // Pengaturan, never through the main screen's console.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .onGloballyPositioned { coords ->
                    consoleHeight = with(density) { coords.size.height.toDp() }
                }
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
                .floatingHeaderCard(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(vertical = 10.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Cari / edit nomor mesin", color = colors.textFaint) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Violet500,
                        unfocusedBorderColor = colors.border,
                        cursorColor = Violet500,
                        focusedContainerColor = colors.bgElevated2,
                        unfocusedContainerColor = colors.bgElevated2,
                    ),
                    shape = RoundedCornerShape(50.dp),
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        state.db[search]?.let { mesin -> loadFrom(search, mesin) }
                    }),
                    singleLine = true,
                )
                Button(
                    onClick = { state.db[search]?.let { mesin -> loadFrom(search, mesin) } },
                    enabled = search.isNotBlank() && state.db[search] != null,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cyan600,
                        disabledContainerColor = colors.bgElevated2,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Mc $search",
                        tint = if (search.isNotBlank() && state.db[search] != null) Color.White else colors.textFaint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
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
