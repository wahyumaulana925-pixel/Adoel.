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
import com.jekael.adoel.ui.theme.AppType
import com.jekael.adoel.ui.theme.Cyan600
import com.jekael.adoel.ui.theme.Dimens
import com.jekael.adoel.ui.theme.LocalAppColors
import com.jekael.adoel.ui.theme.Violet500

/** Bulk quick-edit for a batch of neighboring machines going up on the same corak at once (see
 * Master Blueprint §4D) — one Corak+Target Yard entry applied to every machine number typed into
 * the console, instead of opening the same dialog N times. */
@Composable
fun BulkGuidedSetupSheet(
    mcNos: List<String>,
    onDismiss: () -> Unit,
    onSave: (corak: String, targetYard: Double?) -> Unit,
) {
    val colors = LocalAppColors.current
    var corakInput by remember(mcNos) { mutableStateOf("") }
    var targetYardInput by remember(mcNos) { mutableStateOf("") }

    FloatingEditDialog(onDismissRequest = onDismiss) {
        Text(
            text = "Atur ${mcNos.size} Mesin Sekaligus",
            style = AppType.DialogTitle.copy(color = colors.textPrimary),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Mc ${mcNos.joinToString(", ")}",
            style = AppType.Caption.copy(color = Violet500),
        )

        Spacer(Modifier.height(16.dp))

        FieldLabel("Corak Baru")
        OutlinedTextField(
            value = corakInput,
            onValueChange = { corakInput = it.uppercase() },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("cth: 34758", color = colors.textFaint) },
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(Dimens.RadiusControl),
            textStyle = AppType.FieldText.copy(color = colors.textPrimary),
            singleLine = true,
        )

        Spacer(Modifier.height(16.dp))

        FieldLabel("Target Yard (Opsional)")
        OutlinedTextField(
            value = targetYardInput,
            onValueChange = { targetYardInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("cth: 303", color = colors.textFaint) },
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(Dimens.RadiusControl),
            textStyle = AppType.FieldText.copy(color = colors.textPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )

        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                border = BorderStroke(1.dp, colors.border),
            ) { Text("Batal") }
            Button(
                onClick = {
                    if (corakInput.isNotBlank()) {
                        val yard = targetYardInput.trim().replace(',', '.').toDoubleOrNull()
                        onSave(corakInput.trim(), yard)
                    }
                },
                enabled = corakInput.isNotBlank(),
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
            ) { Text("Simpan Semua", fontWeight = FontWeight.SemiBold) }
        }
    }
}
