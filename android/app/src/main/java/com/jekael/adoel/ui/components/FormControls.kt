package com.jekael.adoel.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.jekael.adoel.ui.theme.LocalAppColors
import com.jekael.adoel.ui.theme.Teal500

@Composable
fun FieldLabel(text: String) {
    val colors = LocalAppColors.current
    Text(
        text = text.uppercase(),
        style = TextStyle(
            fontSize = 11.sp,
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
        focusedBorderColor = Teal500,
        unfocusedBorderColor = colors.border,
        cursorColor = Teal500,
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
            kotlinx.coroutines.delay(180)
            onDismissRequest()
        }
    }

    Dialog(
        onDismissRequest = { requestClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        DisableDialogWindowAnimation()

        Box(modifier = Modifier.fillMaxSize()) {
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
                        .shadow(elevation = 24.dp, shape = RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(colors.bgElevated)
                        .padding(20.dp),
                    content = content,
                )
            }
        }
    }
}
