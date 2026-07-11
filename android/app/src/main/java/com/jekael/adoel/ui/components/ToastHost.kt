package com.jekael.adoel.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.ui.theme.*
import com.jekael.adoel.viewmodel.ToastState
import kotlinx.coroutines.delay

@Composable
fun ToastHost(
    toast: ToastState?,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    LaunchedEffect(toast?.key) {
        if (toast != null) {
            delay(Motion.TOAST_VISIBLE_MS)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
    ) {
        toast?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.4f))
                        .background(colors.bgElevated2, RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = it.msg,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    if (it.undoAction != null) {
                        TextButton(
                            onClick = {
                                it.undoAction.invoke()
                                onDismiss()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "UNDO",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Cyan400,
                                    letterSpacing = 1.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
