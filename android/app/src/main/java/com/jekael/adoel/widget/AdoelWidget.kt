package com.jekael.adoel.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jekael.adoel.MainActivity
import com.jekael.adoel.data.DoffRepository
import com.jekael.adoel.data.UrgencyLevel
import com.jekael.adoel.data.formatDeltaMin
import com.jekael.adoel.data.nearestUpcoming
import com.jekael.adoel.data.nowAbsMin
import com.jekael.adoel.data.urgencyLevel
import com.jekael.adoel.ui.theme.Amber500
import com.jekael.adoel.ui.theme.Cyan500
import com.jekael.adoel.ui.theme.Cyan950
import com.jekael.adoel.ui.theme.Orange500
import com.jekael.adoel.ui.theme.Red500
import com.jekael.adoel.ui.theme.Red900
import com.jekael.adoel.ui.theme.Zinc50
import com.jekael.adoel.ui.theme.Zinc950

/**
 * Home-screen widget showing the single machine most in need of attention right now (earliest
 * overdue, else soonest upcoming), so an operator can check without opening the app. Reads its
 * own [DoffRepository] instance directly — same independent-access pattern already used by
 * [com.jekael.adoel.notification.DoffActionReceiver] outside the Activity/ViewModel scope.
 */
class AdoelWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = DoffRepository(context).load()
        val now = nowAbsMin()
        val nearest = nearestUpcoming(state.estimasi, now)

        provideContent {
            val (bg, accent) = when {
                nearest == null -> Zinc950 to Zinc50
                else -> when (urgencyLevel(nearest.estAbsMin - now)) {
                    UrgencyLevel.CALM -> Cyan950 to Cyan500
                    UrgencyLevel.SOON -> Zinc950 to Amber500
                    UrgencyLevel.IMMINENT -> Zinc950 to Orange500
                    UrgencyLevel.OVERDUE -> Red900 to Red500
                }
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
            ) {
                Text(
                    text = "Adoel.",
                    style = TextStyle(color = ColorProvider(accent), fontSize = 11.sp, fontWeight = FontWeight.Bold),
                )
                if (nearest == null) {
                    Text(
                        text = "Tidak ada estimasi",
                        style = TextStyle(color = ColorProvider(Zinc50), fontSize = 13.sp),
                    )
                } else {
                    val remaining = nearest.estAbsMin - now
                    Text(
                        text = "Mc ${nearest.mcNo}",
                        style = TextStyle(color = ColorProvider(Zinc50), fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = if (remaining <= 0) "Siap doff" else "${formatDeltaMin(remaining)} lagi",
                        style = TextStyle(color = ColorProvider(accent), fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    )
                }
            }
        }
    }
}
