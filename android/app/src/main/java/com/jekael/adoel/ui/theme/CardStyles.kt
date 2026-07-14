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

// Soft shadow in both modes — now that dark mode's background (see DarkBg in Theme.kt) is an
// actual dark gray rather than near-black, a shadow reads against it same as light mode. Kept
// small/subtle either way: this is a secondary depth cue layered on top of the tonal system
// below, not a replacement for it.
private val CardShadowElevation = 3.dp

@Composable
private fun Modifier.softCardShadow(shape: RoundedCornerShape): Modifier =
    this.shadow(elevation = CardShadowElevation, shape = shape, clip = false)

/**
 * Tonal elevation as the primary depth cue: depth/hierarchy is read from how much a surface's
 * background tone has lifted off [LocalAppColors.bg]. A thin border stands in for the extra
 * separation a stronger shadow would give; [softCardShadow] layers a subtle shadow on top in
 * both modes.
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
