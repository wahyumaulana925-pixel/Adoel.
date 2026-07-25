package com.jekael.adoel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.jekael.adoel.ui.theme.AppType
import com.jekael.adoel.ui.theme.Dimens
import com.jekael.adoel.ui.theme.Emerald500
import com.jekael.adoel.ui.theme.Zinc100

/** "Selesai Shift" celebration overlay — a checkmark scaling in over a dimming backdrop.
 *
 * [checkScale]/[backdropAlpha] are lambdas (not plain Floats) so the underlying Animatable's
 * `.value` is read inside this composable's own graphicsLayer{}/background draw, not at
 * MainScreen's call site — reading it there would resubscribe MainScreen's whole recomposition
 * scope to every animation frame of this overlay. The backdrop's background(...) call itself still
 * runs at composition time, so it reads backdropAlpha() directly (not inside a graphicsLayer),
 * which is fine since Box's own recomposition scope is this composable's, not MainScreen's. */
@Composable
internal fun ShiftFinishedOverlay(checkScale: () -> Float, backdropAlpha: () -> Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f * backdropAlpha())),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Emerald500,
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer { scaleX = checkScale(); scaleY = checkScale(); alpha = backdropAlpha() },
            )
            Spacer(Modifier.height(Dimens.Space12))
            Text(
                "Shift Selesai",
                style = AppType.DialogTitle.copy(color = Zinc100),
                modifier = Modifier.graphicsLayer { alpha = backdropAlpha() },
            )
        }
    }
}
