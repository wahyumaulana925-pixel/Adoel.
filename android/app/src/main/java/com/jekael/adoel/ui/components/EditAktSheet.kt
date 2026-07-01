package com.jekael.adoel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.AktualEntry
import com.jekael.adoel.data.DoffState
import com.jekael.adoel.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAktSheet(
    aktualId: Int?,
    state: DoffState,
    onClose: () -> Unit,
    onSave: (id: Int, ket: String) -> Unit,
    onHapus: (entry: AktualEntry) -> Unit,
) {
    if (aktualId == null) return
    val entry = state.aktual.find { it.id == aktualId } ?: return
    val mesin = state.db[entry.mcNo]
    val corak = entry.corakOverride ?: mesin?.corak ?: "—"

    var valInput by remember(aktualId) { mutableStateOf(entry.ket) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(aktualId) {
        delay(100)
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Zinc900,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Zinc700),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Mc ${entry.mcNo}",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, color = Zinc100),
                )
                Text(
                    text = corak,
                    style = TextStyle(fontSize = 14.sp, color = Zinc400),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Text(
                    text = entry.jam,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Zinc500),
                )
            }

            Spacer(Modifier.height(20.dp))

            FieldLabel("Keterangan")
            OutlinedTextField(
                value = valInput,
                onValueChange = { valInput = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = Zinc100, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val k = valInput.trim()
                    if (k.isNotEmpty()) onSave(entry.id, k)
                }),
                singleLine = true,
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onHapus(entry) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
                    border = BorderStroke(1.dp, Zinc700),
                ) { Text("Hapus") }
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Zinc400),
                    border = BorderStroke(1.dp, Zinc700),
                ) { Text("Batal") }
                Button(
                    onClick = {
                        val k = valInput.trim()
                        if (k.isNotEmpty()) onSave(entry.id, k)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                ) { Text("Simpan", fontWeight = FontWeight.SemiBold) }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
