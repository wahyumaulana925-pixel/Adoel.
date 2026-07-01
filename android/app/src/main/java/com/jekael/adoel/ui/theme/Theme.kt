package com.jekael.adoel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Zinc950 = Color(0xFF09090B)
val Zinc900 = Color(0xFF18181B)
val Zinc800 = Color(0xFF27272A)
val Zinc700 = Color(0xFF3F3F46)
val Zinc600 = Color(0xFF52525B)
val Zinc500 = Color(0xFF71717A)
val Zinc400 = Color(0xFFA1A1AA)
val Zinc300 = Color(0xFFD4D4D8)
val Zinc200 = Color(0xFFE4E4E7)
val Zinc100 = Color(0xFFF4F4F5)

val Cyan400 = Color(0xFF22D3EE)
val Cyan500 = Color(0xFF06B6D4)
val Cyan600 = Color(0xFF0891B2)
val Cyan700 = Color(0xFF0E7490)
val Cyan800 = Color(0xFF155E75)
val Cyan950 = Color(0xFF083344)

val Teal400 = Color(0xFF2DD4BF)
val Teal500 = Color(0xFF14B8A6)
val Teal600 = Color(0xFF0D9488)

val Amber400 = Color(0xFFFBBF24)
val Amber500 = Color(0xFFF59E0B)
val Amber700 = Color(0xFFB45309)

val Orange400 = Color(0xFFFB923C)
val Orange500 = Color(0xFFF97316)
val Orange700 = Color(0xFFC2410C)

val Red400 = Color(0xFFF87171)
val Red500 = Color(0xFFEF4444)
val Red700 = Color(0xFFB91C1C)
val Red900 = Color(0xFF7F1D1D)

val Violet500 = Color(0xFF8B5CF6)
val Sky500 = Color(0xFF0EA5E9)
val Emerald500 = Color(0xFF10B981)

private val DarkColors = darkColorScheme(
    primary = Cyan500,
    onPrimary = Zinc950,
    background = Zinc950,
    surface = Zinc900,
    onBackground = Zinc100,
    onSurface = Zinc100,
)

@Composable
fun AdoelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
