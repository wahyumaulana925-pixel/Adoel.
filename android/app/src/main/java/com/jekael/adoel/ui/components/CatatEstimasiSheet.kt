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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Batch "Catat Estimasi" entry. Operators type estimates for every machine in their
 * responsibility one line at a time, right at the start of a shift — they already know
 * the machine number and the value by heart, so a picker only slows that down. Format is
 * "mcNo value" (e.g. "55 25", "67 297.4", "80 18.19"); machine type is looked up
 * automatically so the same field works for TAPPET/CAM (minutes), D405 (yard) and D408
 * (jam counter). The sheet stays open after each successful submit so the operator can
 * keep entering machines without reopening it — closing is an explicit action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatatEstimasiSheet(
    onClose: () -> Unit,
    onSubmit: (rawLine: String) -> Boolean,
) {
    var line by remember { mutableStateOf("") }
    var count by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
    }

    fun submit() {
        if (line.isBlank()) return
        if (onSubmit(line)) {
            line = ""
            count++
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Catat Estimasi",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black, color = Zinc100),
                )
                if (count > 0) {
                    Text(
                        text = "$count mesin diinput",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Amber400),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Ketik nomor mesin + estimasi, lalu kirim. Contoh: 55 25 (menit) · 67 297.4 (yard) · 80 18.19 (jam)",
                style = TextStyle(fontSize = 12.sp, color = Zinc500, lineHeight = 16.sp),
            )

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = line,
                    onValueChange = { line = it.uppercase() },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    placeholder = { Text("cth: 55 25", color = Zinc600) },
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(50.dp),
                    textStyle = TextStyle(
                        color = Zinc100,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                    singleLine = true,
                )
                Button(
                    onClick = { submit() },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Zinc950),
                    contentPadding = PaddingValues(0.dp),
                ) { SendIcon() }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Zinc800, contentColor = Zinc300),
            ) {
                Text("Selesai", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
