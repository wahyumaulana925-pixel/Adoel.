package com.jekael.adoel.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Diagonal twill-weave line texture — a nod to this app's fabric-loom domain (it tracks Water Jet
 * Loom doffing) without literal photography, since the project has no image-asset pipeline. Color
 * always comes from [LocalAppColors.border], so a single implementation adapts to both themes
 * instead of needing separate light/dark texture assets.
 *
 * Rendered as a small tile drawn ONCE and reused as a repeating [android.graphics.BitmapShader],
 * cached globally by (color, tile size, stroke width) — not a fresh [androidx.compose.ui.graphics.Path]
 * of dozens of line segments re-stroked on every single surface on every frame. This texture ended
 * up on every card/header/dialog/page background app-wide, including RadarCard which already
 * carries several of its own per-frame animations (OVERDUE pulse, entrance fade, swipe, long-press
 * charge) — the per-instance Path approach measurably added up once multiplied across everything
 * visible at once; a shared tiled-bitmap brush costs one GPU fill per surface instead.
 */
private val twillTileCache = mutableMapOf<String, ImageBitmap>()

private fun twillTile(colorArgb: Int, tileSizePx: Int, strokePx: Float): ImageBitmap {
    val key = "$colorArgb-$tileSizePx-$strokePx"
    return twillTileCache.getOrPut(key) {
        val bmp = android.graphics.Bitmap.createBitmap(tileSizePx, tileSizePx, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = strokePx
        }
        // A single diagonal stroke through the tile; REPEAT tiling turns it into a continuous
        // sweep of parallel diagonal lines across whatever surface it's painted onto.
        canvas.drawLine(0f, tileSizePx.toFloat(), tileSizePx.toFloat(), 0f, paint)
        bmp.asImageBitmap()
    }
}

@Composable
private fun Modifier.twillTexture(alpha: Float, spacing: Dp = 7.dp, strokeWidth: Dp = 0.8.dp): Modifier {
    val color = LocalAppColors.current.border
    val density = LocalDensity.current
    val tileSizePx = with(density) { spacing.toPx() }.toInt().coerceAtLeast(2)
    val strokePx = with(density) { strokeWidth.toPx() }
    val colorArgb = remember(color, alpha) { color.copy(alpha = alpha).toArgb() }
    val brush = remember(colorArgb, tileSizePx, strokePx) {
        val tile = twillTile(colorArgb, tileSizePx, strokePx)
        ShaderBrush(
            android.graphics.BitmapShader(
                tile.asAndroidBitmap(),
                android.graphics.Shader.TileMode.REPEAT,
                android.graphics.Shader.TileMode.REPEAT,
            ),
        )
    }
    return this.drawBehind { drawRect(brush = brush) }
}

/** For surfaces with text sitting on top — cards, header, console, dialogs/sheets. Kept faint so
 * it never competes with legibility. Brief caps all fabric texture at 2-5% opacity; this sits at
 * the low end since text renders directly over it. */
@Composable
fun Modifier.fabricTextureSubtle(): Modifier = twillTexture(alpha = 0.03f)

/** For plain surfaces with no text directly on them — a screen's raw page background. Sits at the
 * top of the brief's 2-5% opacity cap (was 14% before — well outside "nyaris tidak terlihat") since
 * nothing needs to stay legible over it. */
@Composable
fun Modifier.fabricTextureBold(): Modifier = twillTexture(alpha = 0.05f)

/** Drop-in replacement for `HorizontalDivider(color = colors.border)` — a dashed rather than solid
 * line, echoing a single thread rather than a plain rule. (The diagonal twill tile above needs
 * real vertical extent to read as diagonal, which a 1dp-tall divider doesn't have — a dashed
 * stroke reads as "thread" at any thickness instead.) */
@Composable
fun WovenDivider(modifier: Modifier = Modifier, thickness: Dp = 1.dp) {
    val color = LocalAppColors.current.border
    Canvas(modifier = modifier.fillMaxWidth().height(thickness)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()), phase = 0f),
        )
    }
}
