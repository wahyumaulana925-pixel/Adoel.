package com.jekael.adoel.ui.components

import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jekael.adoel.data.DoffState
import com.jekael.adoel.data.formatYard
import com.jekael.adoel.data.minOfDayToTimeStr
import com.jekael.adoel.data.parseJam
import com.jekael.adoel.data.standarisasiKeterangan
import com.jekael.adoel.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** ket tersimpan sebagai "jam(extra)" atau cuma "jam" kalau tanpa keterangan tambahan (lihat
 * prosesBarisUmum di DoffViewModel.kt) — field Jam & Keterangan di sheet ini dulu digabung
 * jadi satu teks bebas berbasis ket, jadi mengedit jam berarti retype semuanya termasuk tanda
 * kurungnya. Dipisah supaya tiap field independen: corak/yard yang sudah benar tidak perlu
 * diketik ulang hanya karena mau mengoreksi jam, dan sebaliknya. */
private fun extractExtraKeterangan(ket: String, jam: String): String {
    if (!ket.startsWith(jam)) return ""
    val rest = ket.removePrefix(jam)
    val m = Regex("""^\(([^)]*)\)$""").matchEntire(rest)
    return m?.groupValues?.get(1) ?: ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAktSheet(
    aktualId: Int?,
    state: DoffState,
    onClose: () -> Unit,
    onSave: (id: Int, jam: String, ket: String, corakOverride: String?, customYard: Double?) -> Unit,
    onDelete: () -> Unit,
    onInvalidYard: () -> Unit = {},
    onInvalidJam: () -> Unit = {},
) {
    if (aktualId == null) return
    val entry = state.aktual.find { it.id == aktualId } ?: return
    val mesin = state.db[entry.mcNo]
    val corakDefault = entry.corakOverride ?: mesin?.corak ?: ""
    val colors = LocalAppColors.current

    var jamInput by remember(aktualId) { mutableStateOf(entry.jam) }
    var ketInput by remember(aktualId) { mutableStateOf(extractExtraKeterangan(entry.ket, entry.jam)) }
    var corakInput by remember(aktualId) { mutableStateOf(corakDefault) }
    var yardInput by remember(aktualId) {
        mutableStateOf(entry.customYard?.let { formatYard(it) } ?: "")
    }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var showCheck by remember { mutableStateOf(false) }

    fun doSave() {
        if (showCheck) return
        val jamMin = parseJam(jamInput.trim())
        if (jamMin == null) {
            onInvalidJam()
            return
        }
        val corakTrim = corakInput.trim()
        val corakOverride = if (corakTrim.isNotEmpty() && corakTrim != (mesin?.corak ?: "")) corakTrim else null
        val yardTrim = yardInput.trim().replace(',', '.')
        if (yardTrim.isNotEmpty() && yardTrim.toDoubleOrNull() == null) {
            onInvalidYard()
            return
        }
        val yardVal = yardTrim.toDoubleOrNull()
        val jamStr = minOfDayToTimeStr(jamMin)
        val extra = standarisasiKeterangan(ketInput.trim())
        val newKet = if (extra.isNotEmpty()) "$jamStr($extra)" else jamStr
        showCheck = true
        scope.launch {
            delay(450)
            onSave(entry.id, jamStr, newKet, corakOverride, yardVal)
        }
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
                    style = AppType.NumberLarge.copy(color = colors.textPrimary),
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete) { TrashIcon() }
            }

            Spacer(Modifier.height(20.dp))

            FieldLabel("Jam")
            OutlinedTextField(
                value = jamInput,
                onValueChange = { jamInput = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                placeholder = { Text("14.30", color = colors.textFaint) },
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                textStyle = AppType.FieldText.copy(color = colors.textPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))

            FieldLabel("Corak")
            OutlinedTextField(
                value = corakInput,
                onValueChange = { corakInput = it },
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                textStyle = AppType.FieldText.copy(color = colors.textPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
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
                shape = RoundedCornerShape(Dimens.RadiusControl),
                textStyle = AppType.FieldText.copy(color = colors.textPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))

            FieldLabel("Keterangan (opsional)")
            OutlinedTextField(
                value = ketInput,
                onValueChange = { ketInput = it },
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                textStyle = AppType.FieldText.copy(color = colors.textPrimary),
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
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(Dimens.RadiusControl),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                    border = BorderStroke(1.dp, colors.border),
                ) { Text("Batal") }
                Button(
                    onClick = { doSave() },
                    enabled = !showCheck,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(Dimens.RadiusControl),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cyan600,
                        disabledContainerColor = Emerald500,
                        disabledContentColor = Color.White,
                    ),
                ) {
                    Crossfade(targetState = showCheck, label = "saveIcon") { checked ->
                        if (checked) CheckIcon() else Text("Simpan", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
    }
}
