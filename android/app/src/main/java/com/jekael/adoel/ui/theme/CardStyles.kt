package com.jekael.adoel.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Soft shadow, light mode only — on a near-black dark background a shadow barely registers (see
// isDark doc on AppColors), so dark mode keeps depth purely from tonal elevation + border, same
// as before this existed. Kept small/subtle: this is a secondary depth cue layered on top of the
// tonal system below, not a replacement for it.
private val CardShadowElevation = 3.dp

@Composable
private fun Modifier.softCardShadow(shape: RoundedCornerShape): Modifier {
    val colors = LocalAppColors.current
    return if (colors.isDark) this else this.shadow(elevation = CardShadowElevation, shape = shape, clip = false)
}

/**
 * Tonal elevation as the primary depth cue: depth/hierarchy is read from how much a surface's
 * background tone has lifted off [LocalAppColors.bg] (Material's dark-theme approach — shadows
 * barely read on a near-black background). A thin border stands in for the separation a shadow
 * would have provided in dark mode; light mode additionally gets a subtle [softCardShadow] on top,
 * since shadows do read there.
 *
 * Floating header/console-bar card. Shared by MainScreenHeader, ConsoleBar, SettingsDrawer's
 * header, and StatistikScreen's header — kept in one place so the look can't drift between them.
 * Border/background always come from [LocalAppColors] at every current call site, so they're read
 * here directly rather than threaded through as parameters.
 */
@Composable
fun Modifier.floatingHeaderCard(): Modifier {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(Dimens.RadiusFloating)
    return this
        .softCardShadow(shape)
        .clip(shape)
        .border(1.dp, colors.border, shape)
        .background(colors.bgElevated)
        .fabricTextureSubtle()
}

/**
 * Tonal list-row card: rounded + background + a thin border, plus a subtle shadow in light mode
 * only (see [floatingHeaderCard] doc). [backgroundColor] carries the main hierarchy signal — a
 * plain row passes [LocalAppColors.bgElevated]/`bgElevated2`, while RadarCard tints it toward its
 * urgency color (see `urgency()` in RadarCard.kt) — but the border is what keeps a card readable
 * as "raised" even where the tonal jump off [LocalAppColors.bg] is subtle (e.g. light theme).
 */
@Composable
fun Modifier.elevatedListCard(backgroundColor: Color): Modifier {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(Dimens.RadiusCard)
    return this
        .softCardShadow(shape)
        .clip(shape)
        .border(1.dp, colors.border, shape)
        .background(backgroundColor)
        .fabricTextureSubtle()
}
