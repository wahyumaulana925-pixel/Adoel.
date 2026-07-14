package com.jekael.adoel.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.jekael.adoel.ui.theme.Cyan500
import com.jekael.adoel.ui.theme.Dimens
import com.jekael.adoel.ui.theme.LocalAppColors
import com.jekael.adoel.ui.theme.Motion
import com.jekael.adoel.ui.theme.fabricTextureSubtle

@Composable
fun FieldLabel(text: String) {
    val colors = LocalAppColors.current
    Text(
        text = text.uppercase(),
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = colors.textMuted,
        ),
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
fun outlinedFieldColors(): TextFieldColors {
    val colors = LocalAppColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Cyan500,
        unfocusedBorderColor = colors.border,
        cursorColor = Cyan500,
        focusedContainerColor = colors.bgElevated2,
        unfocusedContainerColor = colors.bgElevated2,
    )
}

/** Strips the platform's default (diagonal scale/fade) window animation from a Compose
 * [Dialog], so only our own AnimatedVisibility transition drives how it enters/exits. */
@Composable
fun DisableDialogWindowAnimation() {
    val view = LocalView.current
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.setWindowAnimations(0)
    }
}

/**
 * A bottom-anchored floating card dialog (replaces ModalBottomSheet) that rises above the
 * keyboard via [Modifier.imePadding] instead of being pinned edge-to-edge, so it always reads
 * as a floating panel — even while typing.
 */
@Composable
fun FloatingEditDialog(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    fun requestClose() {
        visible = false
    }
    LaunchedEffect(visible) {
        if (!visible) {
            kotlinx.coroutines.delay(Motion.DIALOG_DISMISS_MS.toLong())
            onDismissRequest()
        }
    }

    Dialog(
        onDismissRequest = { requestClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        DisableDialogWindowAnimation()

        // BoxWithConstraints (not a plain Box) so the card below can cap its own height against
        // the actual available space — without a cap, a form tall enough (plus the keyboard's
        // own imePadding inset) could push its bottom Batal/Reset/Simpan row off-screen with no
        // way to reach it. The heightIn + verticalScroll pair keeps the card fully on-screen and
        // lets the user scroll to the buttons instead.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxCardHeight = maxHeight * 0.85f
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(160)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { requestClose() },
                )
            }
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(260)) + fadeIn(tween(220)),
                exit = slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200)) + fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .heightIn(max = maxCardHeight)
                        .clip(RoundedCornerShape(Dimens.RadiusFloating))
                        .border(1.dp, colors.border, RoundedCornerShape(Dimens.RadiusFloating))
                        .background(colors.bgElevated)
                        .fabricTextureSubtle()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    content = content,
                )
            }
        }
    }
}
