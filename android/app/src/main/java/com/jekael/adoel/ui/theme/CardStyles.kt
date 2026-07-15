package com.jekael.adoel.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

// Soft shadow in both modes — now that dark mode's background (see DarkBg in Theme.kt) is an
// actual dark gray rather than near-black, a shadow reads against it same as light mode. Kept
// small/subtle either way: this is a secondary depth cue layered on top of the tonal system
// below, not a replacement for it.
private val CardShadowElevation = 3.dp

@Composable
private fun Modifier.softCardShadow(shape: RoundedCornerShape): Modifier =
    this.shadow(elevation = CardShadowElevation, shape = shape, clip = false)

// Faint "light falling from above" finish — a vertical gradient lightening toward the top plus a
// hairline highlight right at the top edge — layered on top of the flat tonal-elevation background
// so cards read as a single premium material instead of a flat color fill. Both effects are subtle
// on purpose (this rides on top of the existing tonal/border/shadow depth cues, not a replacement
// for them) and work unmodified against any base color, including RadarCard's per-urgency tint.
// Not private: ConfirmDialog uses its own smaller/rounder shape (20.dp, not RadiusFloating/
// RadiusCard) so it can't go through floatingHeaderCard()/elevatedListCard() below, but should
// still get the same premium finish as every other floating surface.
@Composable
fun Modifier.premiumSurface(base: Color): Modifier = this
    .background(
        Brush.verticalGradient(
            0f to lerp(base, Color.White, 0.05f),
            0.5f to base,
        ),
    )
    .drawWithContent {
        drawContent()
        drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = Offset(0f, 0.5.dp.toPx()),
            end = Offset(size.width, 0.5.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
        )
    }

/**
 * Tonal elevation as the primary depth cue: depth/hierarchy is read from how much a surface's
 * background tone has lifted off [LocalAppColors.bg]. A thin border stands in for the extra
 * separation a stronger shadow would give; [softCardShadow] layers a subtle shadow on top in
 * both modes.
 *
 * Floating header/console-bar card. Shared by MainScreenHeader, ConsoleBar, SettingsDrawer's
 * header, StatistikScreen's header, and [com.jekael.adoel.ui.components.FloatingEditDialog] —
 * kept in one place so the look can't drift between them. Border/background always come from
 * [LocalAppColors] at every current call site, so they're read here directly rather than threaded
 * through as parameters.
 *
 * [textured] defaults on for the outer chrome (header/console/panel headers) but is switched off
 * for dense text-entry surfaces (FloatingEditDialog) — the twill texture sitting directly behind
 * a form's own text competed with fly-waste dust actually stuck to the screen on the factory
 * floor, undercutting legibility exactly where it matters most (Master Blueprint §2B).
 */
@Composable
fun Modifier.floatingHeaderCard(textured: Boolean = true): Modifier {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(Dimens.RadiusFloating)
    return this
        .softCardShadow(shape)
        .clip(shape)
        .border(1.dp, colors.border, shape)
        .premiumSurface(colors.bgElevated)
        .let { if (textured) it.fabricTextureSubtle() else it }
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
        .premiumSurface(backgroundColor)
        .fabricTextureSubtle()
}
