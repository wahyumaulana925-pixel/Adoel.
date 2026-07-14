package com.jekael.adoel.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, LIGHT, DARK }

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
val Zinc50 = Color(0xFFFAFAFA)

val Cyan400 = Color(0xFF22D3EE)
val Cyan500 = Color(0xFF06B6D4)
val Cyan600 = Color(0xFF0891B2)
val Cyan700 = Color(0xFF0E7490)

val Blue400 = Color(0xFF60A5FA)
val Blue500 = Color(0xFF3B82F6)
val Blue700 = Color(0xFF1D4ED8)

val Teal500 = Color(0xFF14B8A6)

val Amber400 = Color(0xFFFBBF24)
val Amber500 = Color(0xFFF59E0B)
val Amber600 = Color(0xFFD97706)
val Amber700 = Color(0xFFB45309)

val Red400 = Color(0xFFF87171)
val Red500 = Color(0xFFEF4444)
val Red700 = Color(0xFFB91C1C)

val Violet500 = Color(0xFF8B5CF6)
val Sky500 = Color(0xFF0EA5E9)
val Emerald500 = Color(0xFF10B981)

/**
 * Semantic, theme-aware neutral tokens. Every non-brand (Zinc-scale) color used across the
 * app should come from here rather than a literal Zinc constant, so that switching between
 * dark/light actually changes the UI instead of just the MaterialTheme.colorScheme (which
 * most of this app's composables never read).
 */
class AppColors(
    val bg: Color,
    val bgElevated: Color,
    val bgElevated2: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textFaint: Color,
    val bannerWarnBg: Color,
    val bannerWarnFg: Color,
    val criticalPulseTarget: Color,
    // Lets card-style modifiers (see CardStyles.kt) apply a soft drop shadow only in light mode —
    // on a near-black background a shadow barely registers, so dark mode keeps depth purely from
    // tonal elevation + border, same as before this flag existed.
    val isDark: Boolean,
)

private val DarkAppColors = AppColors(
    isDark = true,
    bg = Zinc950,
    bgElevated = Zinc900,
    bgElevated2 = Zinc800,
    border = Zinc700,
    textPrimary = Zinc100,
    // Shifted one Zinc step lighter than the original 400/500/600 ladder — textFaint's old
    // 2.57:1 contrast against Zinc950 fell well short of WCAG AA; this ladder targets
    // 13.46:1 / 7.76:1 / 4.12:1 respectively while keeping the three tiers visually distinct.
    textSecondary = Zinc300,
    textMuted = Zinc400,
    textFaint = Zinc500,
    bannerWarnBg = Color(0xFF292007),
    bannerWarnFg = Amber400,
    criticalPulseTarget = Color(0xFF3A1414),
)

// Warm-neutral light palette — a soft paper/cream base instead of stark white, kept low-glare
// on purpose (a plain white bg made list cards/header/console read as almost the same tone as
// the page once shadows were replaced with tonal elevation — see elevatedListCard/floatingHeaderCard).
// Text tokens are left on the cool Zinc scale: luminance (what drives WCAG contrast) barely
// shifts moving Zinc100→WarmBg300 below, so the ratios noted on textFaint below still hold
// (textPrimary/Secondary/Muted comfortably >7:1, textFaint ~4:1, same as before this change).
val WarmBg300 = Color(0xFFEDEAE4)
val WarmBg100 = Color(0xFFF7F5F1)
val WarmBorder = Color(0xFFD9D3C7)

private val LightAppColors = AppColors(
    isDark = false,
    bg = WarmBg300,
    bgElevated = WarmBg100,
    bgElevated2 = Color(0xFFFFFFFF),
    border = WarmBorder,
    textPrimary = Zinc900,
    textSecondary = Zinc700,
    textMuted = Zinc600,
    // textFaint alone moved 400→500 (2.33:1 → 4.40:1) — textSecondary/textMuted above were
    // already well above WCAG AA (9.50:1 / 7.03:1) and don't need to shift.
    textFaint = Zinc500,
    bannerWarnBg = Color(0xFFFEF3C7),
    bannerWarnFg = Amber700,
    criticalPulseTarget = Color(0xFFFECACA),
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

private val DarkScheme = darkColorScheme(
    primary = Cyan500,
    onPrimary = Zinc950,
    background = Zinc950,
    surface = Zinc900,
    onBackground = Zinc100,
    onSurface = Zinc100,
)

private val LightScheme = lightColorScheme(
    primary = Cyan600,
    onPrimary = Zinc50,
    background = Zinc100,
    surface = Zinc50,
    onBackground = Zinc900,
    onSurface = Zinc900,
)

// Material3's own default Typography() (Roboto) with every role's fontFamily swapped to Inter,
// so stock M3 components (Button labels, AlertDialog title/text, OutlinedTextField label) pick
// up Inter too, not just the ad hoc AppType tokens most of this app's composables read instead.
private val DefaultTypography = Typography()
private val AdoelTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = InterFontFamily),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = InterFontFamily),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = InterFontFamily),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = InterFontFamily),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = InterFontFamily),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = InterFontFamily),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = InterFontFamily),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = InterFontFamily),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = InterFontFamily),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = InterFontFamily),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = InterFontFamily),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = InterFontFamily),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = InterFontFamily),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = InterFontFamily),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = InterFontFamily),
)

@Composable
fun resolveDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun AdoelTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = resolveDarkTheme(themeMode)
    CompositionLocalProvider(
        LocalAppColors provides if (dark) DarkAppColors else LightAppColors,
        LocalIndication provides FabricWaveIndication,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AdoelTypography,
            content = content,
        )
    }
}
