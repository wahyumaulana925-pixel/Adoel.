package com.jekael.adoel.ui.theme

import androidx.compose.ui.unit.dp

/** Shared dimension tokens for values that repeat across files. Corner radii that only appear
 * in one component stay inline there — these are the ones that define a cross-screen visual
 * identity (every floating bar, every list card) or physics that must feel identical everywhere
 * (swipe gestures on Estimasi/Doffing/Statistik cards). */
object Dimens {
    /** Corner radius of the floating bars: header, console, panel headers. */
    val RadiusFloating = 28.dp

    /** Corner radius of list cards: radar, doffing rows, shift cards, settings machine rows. */
    val RadiusCard = 14.dp

    /** Horizontal drag distance at which a card swipe commits its action. Shared between
     * RadarCard's bespoke swipe and the generic SwipeableCard so all cards feel identical. */
    val SwipeThreshold = 88.dp

    /** Maximum distance a card can be dragged past its resting position. */
    val SwipeMax = 132.dp
}
