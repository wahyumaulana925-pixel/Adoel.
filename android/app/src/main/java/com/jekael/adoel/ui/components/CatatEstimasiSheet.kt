package com.jekael.adoel.ui.components

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
import com.jekael.adoel.data.DoffState
import com.jekael.adoel.data.MesinTipe
import com.jekael.adoel.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Guided "Catat Estimasi" form: pick a machine, then the input field label/hint/keyboard
 * changes automatically based on the machine's type (TAPPET/CAM = duration, D405 = yard,
 * D408 = jam counter). Submits by building the same "mcNo rawInput" string the command-line
 * path uses, so it reuses DoffViewModel.prosesBarisKondisiMesin as-is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatatEstimasiSheet(
    state: DoffState,
    onClose: () -> Unit,
    onSubmit: (mcNo: String, rawInput: String) -> Unit,
) {
    var selectedMcNo by remember { mutableStateOf<String?>(null) }
    var value by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(selectedMcNo) {
        if (selectedMcNo != null) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    val mesin = selectedMcNo?.let { state.db[it] }

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
                text = "Catat Estimasi",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black, color = Zinc100),
                modifier = Modifier.padding(bottom = 16.dp),
            )

            FieldLabel("Nomor mesin")
            MachinePicker(
                db = state.db,
                selectedMcNo = selectedMcNo,
                onSelect = { selectedMcNo = it; value = "" },
            )

            if (mesin != null) {
                Spacer(Modifier.height(16.dp))

                val (label, hint) = when (mesin.tipe) {
                    MesinTipe.TAPPET, MesinTipe.CAM -> "Durasi tersisa (menit)" to "cth: 45"
                    MesinTipe.D405 -> "Yard berjalan" to "cth: 150"
                    MesinTipe.D408 -> "Jam counter" to "cth: 14.30"
                }

                FieldLabel(label)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text(hint, color = Zinc600) },
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Zinc100, fontSize = 16.sp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (mesin.tipe == MesinTipe.D405) androidx.compose.ui.text.input.KeyboardType.Decimal
                                       else androidx.compose.ui.text.input.KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (value.isNotBlank()) onSubmit(selectedMcNo!!, value.trim())
                    }),
                    singleLine = true,
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { if (value.isNotBlank()) onSubmit(selectedMcNo!!, value.trim()) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Zinc950),
                    enabled = value.isNotBlank(),
                ) {
                    Text("Simpan Estimasi", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold))
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
