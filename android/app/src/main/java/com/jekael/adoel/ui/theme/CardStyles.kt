package com.jekael.adoel.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Tonal elevation, not shadow: depth/hierarchy is read from how much a surface's background tone
 * has lifted off [LocalAppColors.bg] (Material's dark-theme approach — shadows barely read on a
 * near-black background anyway, and unlike [androidx.compose.ui.draw.shadow] this never needs its
 * own composited RenderNode layer, so it can't visibly lag a frame behind its content when a
 * LazyColumn item is freshly composed scrolling into view). A thin border stands in for the
 * separation a shadow would have provided.
 *
 * Floating header/console-bar card. Shared by MainScreenHeader, ConsoleBar, SettingsDrawer's
 * header, and StatistikScreen's header — kept in one place so the look can't drift between them.
 * Border/background always come from [LocalAppColors] at every current call site, so they're read
 * here directly rather than threaded through as parameters.
 */
@Composable
fun Modifier.floatingHeaderCard(): Modifier {
    val colors = LocalAppColors.current
    return this
        .clip(RoundedCornerShape(Dimens.RadiusFloating))
        .border(1.dp, colors.border, RoundedCornerShape(Dimens.RadiusFloating))
        .background(colors.bgElevated)
}

/**
 * Tonal list-row card: rounded + background, no shadow (see [floatingHeaderCard] doc for why).
 * [backgroundColor] carries the only hierarchy signal now — a plain row passes [LocalAppColors.bgElevated]/
 * `bgElevated2`, while RadarCard tints it toward its urgency color (see `urgency()` in RadarCard.kt).
 */
fun Modifier.elevatedListCard(backgroundColor: Color): Modifier =
    this
        .clip(RoundedCornerShape(Dimens.RadiusCard))
        .background(backgroundColor)
