package com.jekael.adoel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jekael.adoel.data.MesinData
import com.jekael.adoel.data.MesinTipe
import com.jekael.adoel.data.absMinToTimeStr
import com.jekael.adoel.data.estAbsD408
import com.jekael.adoel.data.estimasiFieldHint
import com.jekael.adoel.data.nowAbsMin
import com.jekael.adoel.data.parseDurasi
import com.jekael.adoel.data.parseJam
import com.jekael.adoel.data.sisaMenitD405
import com.jekael.adoel.ui.theme.AppType
import com.jekael.adoel.ui.theme.Cyan600
import com.jekael.adoel.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

/** Terpandu (guided) ESTIMASI entry — Batch 4. One field whose label/keyboard adapt to the tapped
 * machine's [MesinTipe], with a live "≈ jam" preview computed from the exact same pure formulas
 * DoffViewModel uses, so what's previewed here is guaranteed to match what actually gets saved.
 * On Simpan, builds the identical "$mcNo $value" string the Teks console would send and hands it
 * to [onSubmit] — the caller is expected to route it through the same handlers.handleCommand path
 * so the overwrite guard, notification scheduling, and toast/haptic feedback all still apply. */
@Composable
fun GuidedEstimasiSheet(
    mcNo: String,
    mesin: MesinData?,
    onDismiss: () -> Unit,
    onSubmit: (value: String) -> Unit,
) {
    val colors = LocalAppColors.current
    val tipe = mesin?.tipe
    var valueInput by remember(mcNo) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(mcNo) {
        delay(100)
        focusRequester.requestFocus()
    }

    FloatingEditDialog(onDismissRequest = onDismiss) {
        Text(
            text = "Update Estimasi — Mc $mcNo",
            style = AppType.DialogTitle.copy(color = colors.textPrimary),
        )

        Spacer(Modifier.height(16.dp))

        if (tipe == null) {
            Text(
                text = "Mc $mcNo belum diatur — atur corak dulu di Pengaturan",
                style = AppType.FieldText.copy(color = colors.textFaint),
            )
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                border = BorderStroke(1.dp, colors.border),
            ) { Text("Tutup") }
            return@FloatingEditDialog
        }

        val hint = estimasiFieldHint(tipe)
        val preview = remember(valueInput, mesin) { previewEstimasi(tipe, valueInput, mesin) }

        FieldLabel(hint.label)
        OutlinedTextField(
            value = valueInput,
            onValueChange = { valueInput = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            placeholder = { Text("cth: ${hint.example}", color = colors.textFaint) },
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(12.dp),
            textStyle = AppType.FieldText.copy(color = colors.textPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (valueInput.isNotBlank()) onSubmit("$mcNo $valueInput") }),
            singleLine = true,
        )

        Spacer(Modifier.height(10.dp))
        Text(
            text = preview?.let { "≈ jam $it" } ?: "Isi untuk melihat perkiraan jam",
            style = AppType.Caption.copy(color = if (preview != null) Cyan600 else colors.textFaint),
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                border = BorderStroke(1.dp, colors.border),
            ) { Text("Batal") }
            Button(
                onClick = { if (valueInput.isNotBlank()) onSubmit("$mcNo $valueInput") },
                enabled = valueInput.isNotBlank(),
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
            ) { Text("Simpan", fontWeight = FontWeight.SemiBold) }
        }
    }
}

private fun previewEstimasi(tipe: MesinTipe, raw: String, mesin: MesinData?): String? {
    if (raw.isBlank()) return null
    val now = nowAbsMin()
    return when (tipe) {
        MesinTipe.TAPPET, MesinTipe.CAM -> parseDurasi(raw)?.let { sisa -> absMinToTimeStr(now + sisa) }
        MesinTipe.D405 -> {
            val target = mesin?.targetYard
            val speed = mesin?.speed
            val yardBerjalan = raw.trim().trimEnd('y', 'Y').replace(',', '.').toDoubleOrNull()
            if (yardBerjalan != null && target != null && speed != null && speed > 0) {
                absMinToTimeStr(now + sisaMenitD405(target, yardBerjalan, speed))
            } else null
        }
        MesinTipe.D408 -> {
            val koreksi = mesin?.koreksi
            val jamMin = parseJam(raw)
            if (jamMin != null && koreksi != null) absMinToTimeStr(estAbsD408(jamMin, koreksi, now)) else null
        }
    }
}
