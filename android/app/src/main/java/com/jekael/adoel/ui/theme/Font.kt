@file:OptIn(ExperimentalTextApi::class)

package com.jekael.adoel.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.jekael.adoel.R

/** Inter (OFL-1.1, see android/app/licenses/inter-OFL.txt), vendored as a single variable-font
 * file — google/fonts no longer ships static per-weight instances for Inter, only
 * `Inter[opsz,wght].ttf`. Each entry below points at that same file with a different `wght`
 * axis setting instead of a different file. On API < 26 the variation setting is ignored and
 * the font falls back to its default named instance (Regular) — an acceptable degradation on
 * the handful of Android 7.0/7.1 (minSdk 24) devices still in the field. */
val InterFontFamily = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.inter_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.inter_variable, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    Font(R.font.inter_variable, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
)
