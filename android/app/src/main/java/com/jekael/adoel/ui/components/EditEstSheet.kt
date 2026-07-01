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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.*
import com.jekael.adoel.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEstSheet(
    mcNo: String?,
    state: DoffState,
    nowAbs: Long,
    onClose: () -> Unit,
    onSave: (rawInput: String, corakOverride: String?) -> Unit,
    onHapus: () -> Unit,
) {
    if (mcNo == null) return
    val est = state.estimasi[mcNo] ?: return
    val mesin = state.db[mcNo] ?: return

    val delta = est.estAbsMin - nowAbs
    val deltaStr = formatDeltaMin(delta)

    var valInput by remember(mcNo) { mutableStateOf("") }
    var corakVal by remember(mcNo) { mutableStateOf(est.corakOverride ?: mesin.corak) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(mcNo) {
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Mc $mcNo",
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, color = Zinc100),
                    )
                    Text(
                        text = mesin.tipe.name,
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Zinc500),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = absMinToTimeStr(est.estAbsMin),
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black, color = Teal400),
                    )
                    Text(
                        text = deltaStr,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (delta < 0) Red400 else Zinc500,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            val hint = when (mesin.tipe) {
                MesinTipe.TAPPET, MesinTipe.CAM -> "menit"
                MesinTipe.D405 -> "yard"
                MesinTipe.D408 -> "jam counter"
            }
            val placeholder = when (mesin.tipe) {
                MesinTipe.D408 -> "cth: 14.30"
                MesinTipe.D405 -> "cth: 150y"
                else -> "cth: 45"
            }

            FieldLabel("Estimasi baru ($hint)")
            OutlinedTextField(
                value = valInput,
                onValueChange = { valInput = it.uppercase() },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                placeholder = { Text(placeholder, color = Zinc600) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    onSave(valInput, corakVal.trim().takeIf { it.isNotEmpty() })
                }),
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = Zinc100, fontSize = 14.sp),
                singleLine = true,
            )

            Spacer(Modifier.height(12.dp))

            FieldLabel("Corak (override sesi ini)")
            OutlinedTextField(
                value = corakVal,
                onValueChange = { corakVal = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(mesin.corak, color = Zinc600) },
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = Zinc100, fontSize = 14.sp),
                singleLine = true,
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onHapus,
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
                    onClick = { onSave(valInput, corakVal.trim().takeIf { it.isNotEmpty() }) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                ) { Text("Simpan", fontWeight = FontWeight.SemiBold) }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = Zinc500,
        ),
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Teal500,
    unfocusedBorderColor = Zinc700,
    cursorColor = Teal500,
    focusedContainerColor = Zinc800,
    unfocusedContainerColor = Zinc800,
)
