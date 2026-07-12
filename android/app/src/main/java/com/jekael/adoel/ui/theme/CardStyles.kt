package com.jekael.adoel.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Floating header/console-bar card: elevated + rounded + a subtle border (shadows alone barely
 * read on a near-black dark background). Shared by MainScreenHeader, ConsoleBar, SettingsDrawer's
 * header, and StatistikScreen's header — kept in one place so the look can't drift between them.
 * Border/background always come from [LocalAppColors] at every current call site, so they're read
 * here directly rather than threaded through as parameters.
 */
@Composable
fun Modifier.floatingHeaderCard(): Modifier {
    val colors = LocalAppColors.current
    return this
        .shadow(elevation = 16.dp, shape = RoundedCornerShape(Dimens.RadiusFloating))
        .clip(RoundedCornerShape(Dimens.RadiusFloating))
        .border(1.dp, colors.border, RoundedCornerShape(Dimens.RadiusFloating))
        .background(colors.bgElevated)
}

/**
 * Elevated list-row card: shadow + rounded + background. [elevation]/[ambientAlpha]/[backgroundColor]
 * are left as parameters (not baked in) since current call sites disagree — RadarCard/DoffingRow/
 * ShiftRow use 5dp @ 0.35 alpha, SettingsDrawer's MesinTab rows use 3dp @ 0.3 alpha, and RadarCard's
 * background is a dynamically computed urgency color, not a constant. Batch 5 reconciles the
 * mismatched values; this only removes the duplicated modifier chain.
 */
fun Modifier.elevatedListCard(
    elevation: Dp,
    backgroundColor: Color,
    ambientAlpha: Float = 0.35f,
): Modifier =
    this
        .shadow(elevation = elevation, shape = RoundedCornerShape(Dimens.RadiusCard), ambientColor = Color.Black.copy(alpha = ambientAlpha))
        .clip(RoundedCornerShape(Dimens.RadiusCard))
        .background(backgroundColor)
