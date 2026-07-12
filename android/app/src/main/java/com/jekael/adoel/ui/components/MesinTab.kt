package com.jekael.adoel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Scrolls behind the floating header/tab-switcher card above instead of being pushed
        // down by it — see the Box-overlay comment on SettingsDrawer's root.
        Spacer(Modifier.height(10.dp + headerHeight + 16.dp))
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

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.forEach { (k, v) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .elevatedListCard(elevation = 3.dp, backgroundColor = colors.bgElevated2, ambientAlpha = 0.3f)
                        .clickable { loadFrom(k, v) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(k, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary), modifier = Modifier.width(32.dp))
                    Text(v.tipe.name, style = TextStyle(fontSize = 11.sp, letterSpacing = 1.sp, color = colors.textMuted), modifier = Modifier.width(56.dp))
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
            if (entries.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("Tidak ditemukan", color = colors.textFaint, style = AppType.FieldText)
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
