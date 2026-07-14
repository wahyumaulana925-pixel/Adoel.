package com.jekael.adoel.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.KETERANGAN_CODES
import com.jekael.adoel.ui.components.CheckIcon
import com.jekael.adoel.ui.components.SendIcon
import com.jekael.adoel.ui.components.SlidingToggle
import com.jekael.adoel.ui.theme.*

/** Floating command bar — mode toggle + (in DOFFING mode) quick keterangan chips + the text
 * input/send row. Reports its own measured height via [onHeightMeasured] so the scrollable list
 * behind it can pad itself to avoid sitting under the card. */
@Composable
internal fun ConsoleBar(
    mode: Mode,
    onModeSelect: (Mode) -> Unit,
    inputStyle: InputStyle,
    input: String,
    onInputChange: (String) -> Unit,
    inputFocus: FocusRequester,
    onSend: () -> Unit,
    // Lambda (not a plain Float) so the .value read of the underlying Animatable happens inside
    // this composable's own graphicsLayer{} block at draw time, not at MainScreen's call site —
    // reading it there would resubscribe MainScreen's whole recomposition scope to every
    // animation frame of the send-button bounce.
    sendScale: () -> Float,
    sendShowCheck: Boolean,
    inputErrorFlash: Boolean,
    inputHint: String?,
    onGuidedStart: (mcNo: String) -> Unit,
    onHeightMeasured: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val density = LocalDensity.current
    // Brief red ring around the input on a rejected command — a toast alone can be missed if the
    // operator glances back at the machine right after hitting send; this fades out on its own so
    // it never needs to be dismissed.
    val errorRingAlpha by animateFloatAsState(
        targetValue = if (inputErrorFlash) 1f else 0f,
        animationSpec = tween(if (inputErrorFlash) 150 else 250),
        label = "inputErrorRing",
    )

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
            // Quick keterangan chips — placed above the toggle (not between toggle and input)
            // so the toggle→input block below keeps a constant position when switching modes;
            // the console card grows/shrinks upward from its bottom anchor, so only the region
            // above the toggle should move, not the toggle/input themselves. Teks-only: Terpandu
            // offers the same codes as tappable buttons inside its own guided step instead.
            if (mode == Mode.AKTUAL && inputStyle == InputStyle.TEKS) {
                KeteranganChips(
                    onPick = { code -> onInputChange((input.trimEnd() + " " + code).trimStart()) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
            }

            // Mode toggle
            SlidingToggle(
                labelLeft = "ESTIMASI",
                labelRight = "DOFFING",
                accessibilityLabel = "Mode Estimasi/Doffing",
                selectedIndex = if (mode == Mode.ESTIMASI) 0 else 1,
                onSelect = { onModeSelect(if (it == 0) Mode.ESTIMASI else Mode.AKTUAL) },
                containerColor = colors.bgElevated2,
                activeColorLeft = Amber500,
                activeColorRight = Cyan600,
                activeTextColorLeft = Zinc950,
                activeTextColorRight = Zinc100,
                inactiveTextColor = colors.textMuted,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))

            if (inputStyle == InputStyle.TEKS) {
                // Command row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { onInputChange(it.uppercase()) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(inputFocus)
                            .border(2.dp, Red500.copy(alpha = errorRingAlpha), RoundedCornerShape(50.dp)),
                        placeholder = {
                            Text(
                                if (mode == Mode.ESTIMASI) "cth: 31 45" else "cth: 31 HB",
                                color = colors.textFaint,
                            )
                        },
                        supportingText = inputHint?.let { hint ->
                            { Text(hint, style = AppType.Caption.copy(color = colors.textFaint)) }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan500,
                            unfocusedBorderColor = colors.border,
                            cursorColor = Cyan500,
                            focusedContainerColor = colors.bgElevated2,
                            unfocusedContainerColor = colors.bgElevated2,
                        ),
                        shape = RoundedCornerShape(50.dp),
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-0.5).sp,
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            // Estimasi commands are always digits (durasi/yard/bacaan jam) — a
                            // numeric keyboard cuts out an extra keyboard-switch tap per command.
                            // Doffing needs letters too (keterangan like HB), so it keeps the
                            // regular keyboard.
                            keyboardType = if (mode == Mode.ESTIMASI) KeyboardType.Number else KeyboardType.Text,
                            imeAction = ImeAction.Send,
                        ),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        singleLine = true,
                    )
                    // Send button — bounces and briefly flashes a checkmark on a successful submit
                    Button(
                        onClick = onSend,
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer { scaleX = sendScale(); scaleY = sendScale() }
                            .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = Cyan600.copy(alpha = 0.6f)),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = if (sendShowCheck) Emerald500 else Cyan600),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Crossfade(targetState = sendShowCheck, label = "sendIcon") { showCheck ->
                            if (showCheck) CheckIcon() else SendIcon()
                        }
                    }
                }
            } else {
                // Terpandu: type just the machine number (no syntax to recall) to open the guided
                // sheet for it directly — this is the entry point for a machine that doesn't have
                // a radar card yet (e.g. its very first estimasi); tapping an existing RadarCard
                // (see MainScreen's onQuickEdit/onDoff wiring) is the faster path once one exists.
                var guidedMcNoInput by remember(mode) { mutableStateOf("") }
                Text(
                    text = "Ketik nomor mesin untuk mulai",
                    style = AppType.Caption.copy(color = colors.textFaint),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = guidedMcNoInput,
                        onValueChange = { guidedMcNoInput = it.filter(Char::isDigit).take(3) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Nomor mesin, cth: 31", color = colors.textFaint) },
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (guidedMcNoInput.isNotBlank()) { onGuidedStart(guidedMcNoInput); guidedMcNoInput = "" }
                        }),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            if (guidedMcNoInput.isNotBlank()) { onGuidedStart(guidedMcNoInput); guidedMcNoInput = "" }
                        },
                        enabled = guidedMcNoInput.isNotBlank(),
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet500),
                    ) { Text("Mulai", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun KeteranganChips(onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        KETERANGAN_CODES.forEach { code ->
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(colors.bgElevated2)
                    .clickable(role = Role.Button) { onPick(code) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = code,
                    style = AppType.LabelSmallBold.copy(color = colors.textSecondary),
                )
            }
        }
    }
}
