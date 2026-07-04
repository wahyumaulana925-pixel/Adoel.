package com.jekael.adoel.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jekael.adoel.data.Estimasi
import com.jekael.adoel.data.MesinData
import com.jekael.adoel.data.UrgencyLevel
import com.jekael.adoel.data.formatDeltaMin
import com.jekael.adoel.data.formatYard
import com.jekael.adoel.data.urgencyLevel
import com.jekael.adoel.ui.theme.Amber500
import com.jekael.adoel.ui.theme.Cyan500
import com.jekael.adoel.ui.theme.Cyan950
import com.jekael.adoel.ui.theme.Orange500
import com.jekael.adoel.ui.theme.Red500
import com.jekael.adoel.ui.theme.Red900
import com.jekael.adoel.ui.theme.Zinc50
import com.jekael.adoel.ui.theme.Zinc950

/** Glance-native, read-only card for one estimasi — no action buttons; the whole row (see
 * AdoelWidget's LazyColumn item wrapper) opens the app on tap instead. */
@Composable
fun WidgetEstimasiCard(est: Estimasi, mesin: MesinData?, now: Long) {
    val remaining = est.estAbsMin - now
    val (bg, accent) = when (urgencyLevel(remaining)) {
        UrgencyLevel.CALM -> Cyan950 to Cyan500
        UrgencyLevel.SOON -> Zinc950 to Amber500
        UrgencyLevel.IMMINENT -> Zinc950 to Orange500
        UrgencyLevel.OVERDUE -> Red900 to Red500
    }
    val corak = est.corakOverride ?: mesin?.corak ?: "—"
    val yard = est.yardOverride ?: mesin?.targetYard
    val corakLine = if (yard != null) "$corak · ${formatYard(yard)}y" else corak

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(12.dp)
            .background(bg)
            .padding(12.dp),
    ) {
        Text("Mc ${est.mcNo}", style = TextStyle(color = ColorProvider(Zinc50), fontSize = 18.sp, fontWeight = FontWeight.Bold))
        Text(corakLine, style = TextStyle(color = ColorProvider(Zinc50), fontSize = 11.sp), maxLines = 1)
        Text(
            text = if (remaining <= 0) "Siap doff" else "${formatDeltaMin(remaining)} lagi",
            style = TextStyle(color = ColorProvider(accent), fontSize = 13.sp, fontWeight = FontWeight.Medium),
        )
    }
}
