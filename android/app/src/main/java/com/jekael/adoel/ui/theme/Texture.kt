package com.jekael.adoel.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Diagonal twill-weave line texture — a nod to this app's fabric-loom domain (it tracks Water Jet
 * Loom doffing) without literal photography, since the project has no image-asset pipeline. Color
 * always comes from [LocalAppColors.border], so a single implementation adapts to both themes
 * instead of needing separate light/dark texture assets. The line geometry is built once per size
 * via [drawWithCache] (not rebuilt every frame), so this stays cheap on cards that also carry their
 * own per-frame animations (OVERDUE pulse, entrance fade).
 */
@Composable
private fun Modifier.twillTexture(alpha: Float, spacing: Dp = 7.dp, strokeWidth: Dp = 0.8.dp): Modifier {
    val lineColor = LocalAppColors.current.border.copy(alpha = alpha)
    val density = LocalDensity.current
    val spacingPx = with(density) { spacing.toPx() }
    val strokePx = with(density) { strokeWidth.toPx() }
    return this.drawWithCache {
        val path = Path().apply {
            var offset = -size.height
            while (offset < size.width) {
                moveTo(offset, 0f)
                lineTo(offset + size.height, size.height)
                offset += spacingPx
            }
        }
        val stroke = Stroke(width = strokePx)
        onDrawBehind {
            drawPath(path, color = lineColor, style = stroke)
        }
    }
}

/** For surfaces with text sitting on top — cards, header, console, dialogs/sheets. Kept faint so
 * it never competes with legibility. */
@Composable
fun Modifier.fabricTextureSubtle(): Modifier = twillTexture(alpha = 0.05f)

/** For plain surfaces with no text directly on them — a screen's raw page background. Can afford
 * to read as an actual weave motif since nothing needs to stay legible over it. */
@Composable
fun Modifier.fabricTextureBold(): Modifier = twillTexture(alpha = 0.14f)
