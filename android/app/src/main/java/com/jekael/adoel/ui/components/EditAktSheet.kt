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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.DoffState
import com.jekael.adoel.data.formatYard
import com.jekael.adoel.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAktSheet(
    aktualId: Int?,
    state: DoffState,
    onClose: () -> Unit,
    onSave: (id: Int, ket: String, corakOverride: String?, customYard: Double?) -> Unit,
    onInvalidYard: () -> Unit = {},
) {
    if (aktualId == null) return
    val entry = state.aktual.find { it.id == aktualId } ?: return
    val mesin = state.db[entry.mcNo]
    val corakDefault = entry.corakOverride ?: mesin?.corak ?: ""
    val colors = LocalAppColors.current

    var valInput by remember(aktualId) { mutableStateOf(entry.ket) }
    var corakInput by remember(aktualId) { mutableStateOf(corakDefault) }
    var yardInput by remember(aktualId) {
        mutableStateOf(entry.customYard?.let { formatYard(it) } ?: "")
    }
    val focusRequester = remember { FocusRequester() }

    fun doSave() {
        val k = valInput.trim()
        if (k.isEmpty()) return
        val corakTrim = corakInput.trim()
        val corakOverride = if (corakTrim.isNotEmpty() && corakTrim != (mesin?.corak ?: "")) corakTrim else null
        val yardTrim = yardInput.trim().replace(',', '.')
        if (yardTrim.isNotEmpty() && yardTrim.toDoubleOrNull() == null) {
            onInvalidYard()
            return
        }
        onSave(entry.id, k, corakOverride, yardTrim.toDoubleOrNull())
    }

    LaunchedEffect(aktualId) {
        delay(100)
        focusRequester.requestFocus()
    }

    FloatingEditDialog(onDismissRequest = onClose) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Mc ${entry.mcNo}",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, color = colors.textPrimary),
                )
                Text(
                    text = entry.jam,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textMuted),
                )
            }

            Spacer(Modifier.height(20.dp))

            FieldLabel("Corak")
            OutlinedTextField(
                value = corakInput,
                onValueChange = { corakInput = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))

            FieldLabel("Panjang / Batas Potong (yard)")
            OutlinedTextField(
                value = yardInput,
                onValueChange = { yardInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    val standar = mesin?.targetYard
                    if (standar != null) {
                        Text("Standar: ${formatYard(standar)}y", color = colors.textFaint)
                    }
                },
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))

            FieldLabel("Keterangan")
            OutlinedTextField(
                value = valInput,
                onValueChange = { valInput = it },
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { doSave() }),
                singleLine = true,
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                    border = BorderStroke(1.dp, colors.border),
                ) { Text("Batal") }
                Button(
                    onClick = { doSave() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                ) { Text("Simpan", fontWeight = FontWeight.SemiBold) }
            }

            Spacer(Modifier.height(8.dp))
    }
}
