package com.jekael.adoel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.DoffState
import com.jekael.adoel.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Guided "Catat Doffing" form: pick a machine, then a plain free-text "Keterangan" field —
 * no suggestions or standardization, the operator can write whatever describes the situation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatatDoffingSheet(
    state: DoffState,
    onClose: () -> Unit,
    onSubmit: (mcNo: String, keterangan: String) -> Unit,
) {
    var selectedMcNo by remember { mutableStateOf<String?>(null) }
    var keterangan by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(selectedMcNo) {
        if (selectedMcNo != null) {
            delay(100)
            focusRequester.requestFocus()
        }
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
            Text(
                text = "Catat Doffing",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black, color = Zinc100),
                modifier = Modifier.padding(bottom = 16.dp),
            )

            FieldLabel("Nomor mesin")
            MachinePicker(
                db = state.db,
                selectedMcNo = selectedMcNo,
                onSelect = { selectedMcNo = it },
            )

            if (selectedMcNo != null) {
                Spacer(Modifier.height(16.dp))

                FieldLabel("Keterangan (opsional)")
                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Tulis kondisi lapangan sebebasnya...", color = Zinc600) },
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Zinc100, fontSize = 15.sp),
                    singleLine = false,
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { onSubmit(selectedMcNo!!, keterangan) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
                ) {
                    Text("Simpan Doffing", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold))
                }
            } else {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Pilih mesin dulu untuk melanjutkan.",
                    style = TextStyle(fontSize = 13.sp, color = Zinc600),
                )
                Spacer(Modifier.height(24.dp))
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
