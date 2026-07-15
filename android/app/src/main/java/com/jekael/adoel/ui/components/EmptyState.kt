package com.jekael.adoel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import com.jekael.adoel.ui.theme.AppType
import com.jekael.adoel.ui.theme.Cyan500
import com.jekael.adoel.ui.theme.LocalAppColors
import com.jekael.adoel.ui.theme.fabricTextureSubtle

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    title: String = "Belum ada mesin yang dipantau",
    subtitle: String = "Masukkan nomor mesin + estimasi di kolom bawah untuk mulai",
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
            // Outer ring redrawn as two dashed circles (same warp/weft two-tone trick as
            // WovenDivider) instead of a flat translucent disc — an idle empty state has nothing
            // urgent to read, so it's a fitting spot to let a "spool of thread" motif carry more
            // of the weaving identity than a screen with live data ever could.
            val warpColor = colors.border
            val weftColor = lerp(colors.border, Cyan500, 0.35f)
            Canvas(modifier = Modifier.size(96.dp)) {
                val strokeW = 1.5.dp.toPx()
                val radius = size.minDimension / 2f - strokeW
                val dash = 10.dp.toPx()
                val gap = 6.dp.toPx()
                drawCircle(
                    color = warpColor,
                    radius = radius,
                    style = Stroke(width = strokeW, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), phase = 0f)),
                )
                drawCircle(
                    color = weftColor,
                    radius = radius,
                    style = Stroke(width = strokeW, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), phase = dash)),
                )
            }
            Box(Modifier.size(72.dp).clip(CircleShape).background(colors.bg))
            Box(Modifier.size(48.dp).clip(CircleShape).background(colors.bgElevated2.copy(alpha = 0.25f)).fabricTextureSubtle())
            Box(Modifier.size(12.dp).clip(CircleShape).background(colors.border))
        }
        Text(
            text = title,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary),
        )
        Text(
            text = subtitle,
            style = AppType.Caption.copy(color = colors.textFaint, lineHeight = 17.sp),
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = TextAlign.Center,
        )
    }
}
