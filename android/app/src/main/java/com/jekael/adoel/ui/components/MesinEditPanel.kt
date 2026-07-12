package com.jekael.adoel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jekael.adoel.data.*
import com.jekael.adoel.ui.theme.*

@Composable
internal fun MesinEditPanel(
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
            style = AppType.NumberLarge.copy(color = colors.textPrimary),
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
            textStyle = AppType.FieldText.copy(color = colors.textPrimary),
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
            textStyle = AppType.FieldText.copy(color = colors.textPrimary),
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
                textStyle = AppType.FieldText.copy(color = colors.textPrimary),
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
                textStyle = AppType.FieldText.copy(color = colors.textPrimary),
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
                colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
            ) { Text("Simpan", fontWeight = FontWeight.SemiBold) }
        }
    }
}
