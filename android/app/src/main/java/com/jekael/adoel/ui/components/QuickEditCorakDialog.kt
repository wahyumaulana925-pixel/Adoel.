package com.jekael.adoel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.formatYard
import com.jekael.adoel.ui.theme.AppType
import com.jekael.adoel.ui.theme.Cyan600
import com.jekael.adoel.ui.theme.Dimens
import com.jekael.adoel.ui.theme.LocalAppColors

/**
 * Fast path for the two machine fields that actually change often on the floor — corak and
 * target yard — reachable by tapping a RadarCard directly instead of the full Pengaturan > Mesin
 * flow (search, open, edit, save, close, close). Deliberately scoped to just these two fields;
 * tipe/speed/koreksi change far less often and stay behind the full Settings editor.
 */
@Composable
fun QuickEditCorakDialog(
    mcNo: String,
    corak: String,
    targetYard: Double?,
    onDismiss: () -> Unit,
    onSave: (corak: String, targetYard: Double?) -> Unit,
    corakShortcuts: List<String>? = null,
    onAddCorakShortcut: (String) -> Unit = {},
    showToast: ((String) -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    var corakInput by remember(mcNo) { mutableStateOf(if (corak == "-") "" else corak) }
    var targetYardInput by remember(mcNo) { mutableStateOf(targetYard?.let { formatYard(it) } ?: "") }

    FloatingEditDialog(onDismissRequest = onDismiss) {
        Text(
            text = "Ganti Cepat — Mc $mcNo",
            style = AppType.DialogTitle.copy(color = colors.textPrimary),
        )

        Spacer(Modifier.height(Dimens.Space16))

        FieldLabel("Corak")
        OutlinedTextField(
            value = corakInput,
            onValueChange = { corakInput = it },
            modifier = Modifier.fillMaxWidth(),
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(Dimens.RadiusControl),
            textStyle = AppType.FieldText.copy(color = colors.textPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        CorakShortcutPicker(
            value = corakInput,
            onSelect = { corakInput = it },
            shortcuts = corakShortcuts,
            onAddShortcut = onAddCorakShortcut,
            showToast = showToast,
        )

        Spacer(Modifier.height(Dimens.Space16))

        FieldLabel("Target Yard")
        OutlinedTextField(
            value = targetYardInput,
            onValueChange = { targetYardInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("opsional", color = colors.textFaint) },
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(Dimens.RadiusControl),
            textStyle = AppType.FieldText.copy(color = colors.textPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )

        Spacer(Modifier.height(Dimens.Space20))

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                border = BorderStroke(1.dp, colors.border),
            ) { Text("Batal") }
            Button(
                onClick = {
                    val trimmed = corakInput.trim().ifEmpty { "-" }
                    // Blank clears the target on purpose; non-blank text that fails to parse falls
                    // back to the previous value instead of silently wiping it on a typo.
                    val yard = if (targetYardInput.isBlank()) {
                        null
                    } else {
                        targetYardInput.trim().replace(',', '.').toDoubleOrNull() ?: targetYard
                    }
                    onSave(trimmed, yard)
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
            ) { Text("Simpan", fontWeight = FontWeight.SemiBold) }
        }
    }
}
