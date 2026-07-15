package com.jekael.adoel.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.ui.theme.*

/** Floating command bar — Pintu Masuk Terpadu Berbasis Nomor Mesin (Master Blueprint §4A): one
 * field for a machine number (or several, comma/space separated for the bulk quick-edit path —
 * see MainScreen's onGuidedStart) and one "Mulai" button. No mode toggle and no free-text command
 * syntax anymore — MainScreen's onGuidedStart inspects machine state to decide what happens next
 * (jump straight to GuidedEstimasi, offer GuidedActionHub, or open the bulk sheet). Reports its
 * own measured height via [onHeightMeasured] so the scrollable list behind it can pad itself to
 * avoid sitting under the card. */
@Composable
internal fun ConsoleBar(
    onGuidedStart: (mcNo: String) -> Unit,
    onHeightMeasured: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val density = LocalDensity.current
    var guidedMcNoInput by remember { mutableStateOf("") }

    fun submit() {
        if (guidedMcNoInput.isNotBlank()) {
            onGuidedStart(guidedMcNoInput)
            guidedMcNoInput = ""
        }
    }

    Box(
        modifier = modifier
            .imePadding()
            .onGloballyPositioned { coords ->
                onHeightMeasured(with(density) { coords.size.height.toDp() })
            }
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .floatingHeaderCard(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(top = 10.dp)
                .navigationBarsPadding()
                .padding(bottom = 10.dp),
        ) {
            Text(
                text = "Ketik nomor mesin untuk mulai — pisahkan koma/spasi untuk banyak mesin",
                style = AppType.Caption.copy(color = colors.textFaint),
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = guidedMcNoInput,
                    // Digits plus comma/space separators — a single machine number still matches
                    // this, multiple (e.g. "31,32,33" or "31 32 33") route to the bulk sheet instead.
                    onValueChange = { guidedMcNoInput = it.filter { c -> c.isDigit() || c == ',' || c == ' ' } },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Nomor mesin, cth: 31 atau 31,32,33", color = colors.textFaint) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Violet500,
                        unfocusedBorderColor = colors.border,
                        cursorColor = Violet500,
                        focusedContainerColor = colors.bgElevated2,
                        unfocusedContainerColor = colors.bgElevated2,
                    ),
                    shape = RoundedCornerShape(50.dp),
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
                    // Text, not Number — the numeric keypad has no comma/space key on most
                    // keyboards, which would make the multi-machine bulk path unreachable.
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    singleLine = true,
                )
                Button(
                    onClick = ::submit,
                    enabled = guidedMcNoInput.isNotBlank(),
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Violet500),
                ) { Text("Mulai", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
