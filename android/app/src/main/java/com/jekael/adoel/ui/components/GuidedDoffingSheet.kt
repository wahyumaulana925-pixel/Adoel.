package com.jekael.adoel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.Estimasi
import com.jekael.adoel.data.KETERANGAN_CODES
import com.jekael.adoel.data.MesinData
import com.jekael.adoel.data.MesinTipe
import com.jekael.adoel.data.formatYard
import com.jekael.adoel.ui.theme.AppType
import com.jekael.adoel.ui.theme.Cyan600
import com.jekael.adoel.ui.theme.LocalAppColors
import com.jekael.adoel.ui.theme.Sky500

private enum class GuidedDoffingStep { CHOOSE, NORMAL, KETERANGAN, COUNTER }

/** Terpandu (guided) DOFFING entry — Batch 5. Step 1 offers big buttons instead of free-text
 * ("Doffing normal" / "Ada keterangan" / D408-only "Update bacaan counter" — the guided,
 * unambiguous replacement for the old console-only "C" token shortcut removed in Batch 6). Every path
 * ends by building the identical command string the Teks console would send for that action and
 * handing it to the matching callback, which the caller routes through the same
 * handlers.handleCommand path Teks uses — so undo, notification cancel/reschedule, and toast/
 * haptic feedback all still apply exactly as they do today. */
@Composable
fun GuidedDoffingSheet(
    mcNo: String,
    mesin: MesinData?,
    estimasi: Estimasi?,
    onDismiss: () -> Unit,
    onSubmitDoffing: (value: String) -> Unit,
    onSubmitCounterUpdate: (value: String) -> Unit,
) {
    val colors = LocalAppColors.current
    var step by remember(mcNo) { mutableStateOf(GuidedDoffingStep.CHOOSE) }
    val standardYard = estimasi?.yardOverride ?: mesin?.targetYard

    FloatingEditDialog(onDismissRequest = onDismiss) {
        Text(
            text = "Catat Doffing — Mc $mcNo",
            style = AppType.DialogTitle.copy(color = colors.textPrimary),
        )
        Spacer(Modifier.height(16.dp))

        when (step) {
            GuidedDoffingStep.CHOOSE -> ChooseStep(
                isD408 = mesin?.tipe == MesinTipe.D408,
                onPickNormal = { step = GuidedDoffingStep.NORMAL },
                onPickKeterangan = { step = GuidedDoffingStep.KETERANGAN },
                onPickCounter = { step = GuidedDoffingStep.COUNTER },
                onCancel = onDismiss,
            )
            GuidedDoffingStep.NORMAL -> NormalYardStep(
                standardYard = standardYard,
                onBack = { step = GuidedDoffingStep.CHOOSE },
                onConfirm = { yard -> onSubmitDoffing("$mcNo $yard") },
            )
            GuidedDoffingStep.KETERANGAN -> KeteranganStep(
                standardYard = standardYard,
                onBack = { step = GuidedDoffingStep.CHOOSE },
                onConfirm = { cmd -> onSubmitDoffing("$mcNo $cmd") },
            )
            GuidedDoffingStep.COUNTER -> CounterUpdateStep(
                onBack = { step = GuidedDoffingStep.CHOOSE },
                onConfirm = { bacaan -> onSubmitCounterUpdate("$mcNo $bacaan") },
            )
        }
    }
}

@Composable
private fun ChooseStep(
    isD408: Boolean,
    onPickNormal: () -> Unit,
    onPickKeterangan: () -> Unit,
    onPickCounter: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BigChoiceButton(label = "Doffing normal", subtitle = "Selesai sesuai target yard", onClick = onPickNormal)
        BigChoiceButton(label = "Ada keterangan", subtitle = "HB, P.LP, P.SN, P.OH, P.EL, P.Sel, atau lainnya", onClick = onPickKeterangan)
        if (isD408) {
            BigChoiceButton(
                label = "Update bacaan counter",
                subtitle = "Bukan doffing — perbarui estimasi dari bacaan jam mesin",
                onClick = onPickCounter,
                accent = Sky500,
            )
        }
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
            border = BorderStroke(1.dp, colors.border),
        ) { Text("Batal") }
    }
}

@Composable
private fun BigChoiceButton(label: String, subtitle: String, onClick: () -> Unit, accent: Color = Cyan600) {
    val colors = LocalAppColors.current
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.bgElevated2),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent))
            Text(subtitle, style = AppType.Caption.copy(color = colors.textFaint))
        }
    }
}

@Composable
private fun NormalYardStep(standardYard: Double?, onBack: () -> Unit, onConfirm: (String) -> Unit) {
    val colors = LocalAppColors.current
    var yardInput by remember(standardYard) {
        mutableStateOf(standardYard?.let { formatYard(it) } ?: "")
    }
    fun step(delta: Double) {
        val current = yardInput.trim().replace(',', '.').toDoubleOrNull() ?: standardYard ?: 0.0
        yardInput = formatYard(current + delta)
    }

    FieldLabel("Yard aktual")
    OutlinedTextField(
        value = yardInput,
        onValueChange = { yardInput = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            if (standardYard != null) Text("Standar: ${formatYard(standardYard)}y", color = colors.textFaint)
        },
        colors = outlinedFieldColors(),
        shape = RoundedCornerShape(12.dp),
        textStyle = AppType.FieldText.copy(color = colors.textPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(-10.0, -5.0, 5.0, 10.0).forEach { delta ->
            OutlinedButton(
                onClick = { step(delta) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                border = BorderStroke(1.dp, colors.border),
            ) { Text(if (delta > 0) "+${delta.toInt()}" else "${delta.toInt()}") }
        }
    }

    Spacer(Modifier.height(20.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
            border = BorderStroke(1.dp, colors.border),
        ) { Text("Kembali") }
        Button(
            onClick = { if (yardInput.isNotBlank()) onConfirm(yardInput.trim()) },
            enabled = yardInput.isNotBlank(),
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
        ) { Text("Simpan", fontWeight = FontWeight.SemiBold) }
    }
}

/** Keterangan needs to cover more than the 6 preset codes (e.g. HB = habis beam, not really a
 * "kendala"/problem, just why the cut yard won't match standard) — chips are a quick-fill for the
 * common ones, but the text field underneath stays freely editable so anything not on that list
 * can still be typed, same freedom as the Teks console. Yard is a plain field (no +/-5/+/-10
 * buttons like NormalYardStep): unlike a normal doff, the variance here isn't a fixed step, it's
 * whatever the actual cut came out to — same as typing e.g. "70" or "+70" in Teks. */
@Composable
private fun KeteranganStep(standardYard: Double?, onBack: () -> Unit, onConfirm: (String) -> Unit) {
    val colors = LocalAppColors.current
    var ket by remember { mutableStateOf("") }
    var yardInput by remember { mutableStateOf("") }

    FieldLabel("Keterangan")
    FlowRowChips(codes = KETERANGAN_CODES, selected = ket.takeIf { it in KETERANGAN_CODES }, onSelect = { ket = it })
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = ket,
        onValueChange = { ket = it.uppercase() },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Ketik keterangan lain jika tidak ada di atas", color = colors.textFaint) },
        colors = outlinedFieldColors(),
        shape = RoundedCornerShape(12.dp),
        textStyle = AppType.FieldText.copy(color = colors.textPrimary),
        singleLine = true,
    )

    Spacer(Modifier.height(14.dp))
    FieldLabel("Yard aktual (opsional)")
    OutlinedTextField(
        value = yardInput,
        onValueChange = { yardInput = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            val hint = if (standardYard != null) "Standar: ${formatYard(standardYard)}y — cth: 70 atau +70" else "cth: 70 atau +70"
            Text(hint, color = colors.textFaint)
        },
        colors = outlinedFieldColors(),
        shape = RoundedCornerShape(12.dp),
        textStyle = AppType.FieldText.copy(color = colors.textPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )

    Spacer(Modifier.height(20.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
            border = BorderStroke(1.dp, colors.border),
        ) { Text("Kembali") }
        Button(
            onClick = {
                val cmd = listOf(ket.trim(), yardInput.trim()).filter { it.isNotEmpty() }.joinToString(" ")
                if (cmd.isNotBlank()) onConfirm(cmd)
            },
            enabled = ket.isNotBlank(),
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
        ) { Text("Simpan", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun CounterUpdateStep(onBack: () -> Unit, onConfirm: (String) -> Unit) {
    val colors = LocalAppColors.current
    var bacaan by remember { mutableStateOf("") }

    FieldLabel("Bacaan jam counter")
    OutlinedTextField(
        value = bacaan,
        onValueChange = { bacaan = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("cth: 12.30", color = colors.textFaint) },
        colors = outlinedFieldColors(),
        shape = RoundedCornerShape(12.dp),
        textStyle = AppType.FieldText.copy(color = colors.textPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )

    Spacer(Modifier.height(20.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
            border = BorderStroke(1.dp, colors.border),
        ) { Text("Kembali") }
        Button(
            onClick = { if (bacaan.isNotBlank()) onConfirm(bacaan.trim()) },
            enabled = bacaan.isNotBlank(),
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Sky500),
        ) { Text("Simpan", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun FlowRowChips(codes: List<String>, selected: String?, onSelect: (String) -> Unit) {
    // Two fixed rows of up to 3 — six keterangan codes always fits, and this avoids pulling in
    // the separate accompanist/foundation FlowRow API just for a list this small and static.
    val rows = codes.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { code ->
                    Box(modifier = Modifier.weight(1f)) {
                        ChipBtn(label = code, selected = selected == code, onClick = { onSelect(code) })
                    }
                }
            }
        }
    }
}
