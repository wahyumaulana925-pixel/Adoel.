package com.jekael.adoel.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jekael.adoel.MainActivity
import com.jekael.adoel.data.DoffRepository
import com.jekael.adoel.data.nowAbsMin
import com.jekael.adoel.data.sortedByNearest
import com.jekael.adoel.ui.theme.Amber500
import com.jekael.adoel.ui.theme.Zinc50
import com.jekael.adoel.ui.theme.Zinc950

/**
 * Home-screen widget: read-only scrollable list of every pending estimasi (nearest/most-overdue
 * first) — tapping anywhere on it opens the app, no in-widget actions. Reads its own
 * [DoffRepository] instance directly — same independent-access pattern already used by
 * [com.jekael.adoel.notification.DoffActionReceiver] outside the Activity/ViewModel scope.
 *
 * Tap-to-open is applied to the header, the empty state, and each row individually (not just the
 * outer Column) because a RemoteViews-backed LazyColumn intercepts touches for its own rows —
 * an ancestor's clickable modifier alone doesn't fire for taps landing on list items.
 */
class AdoelWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = DoffRepository(context).load()
        val now = nowAbsMin()
        val sorted = sortedByNearest(state.estimasi)
        val openApp = actionStartActivity(Intent(context, MainActivity::class.java))

        provideContent {
            Column(modifier = GlanceModifier.fillMaxSize().background(Zinc950)) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clickable(openApp),
                ) {
                    Text(
                        text = "Adoel",
                        style = TextStyle(color = ColorProvider(Zinc50), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = ".",
                        style = TextStyle(color = ColorProvider(Amber500), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    )
                }
                if (sorted.isEmpty()) {
                    Box(modifier = GlanceModifier.fillMaxSize().padding(12.dp).clickable(openApp)) {
                        Text(
                            text = "Tidak ada estimasi",
                            style = TextStyle(color = ColorProvider(Zinc50), fontSize = 13.sp),
                        )
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(items = sorted, itemId = { it.mcNo.hashCode().toLong() }) { est ->
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .clickable(openApp),
                            ) {
                                WidgetEstimasiCard(est = est, mesin = state.db[est.mcNo], now = now)
                            }
                        }
                    }
                }
            }
        }
    }
}
