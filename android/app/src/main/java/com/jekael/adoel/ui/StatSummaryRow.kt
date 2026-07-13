package com.jekael.adoel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jekael.adoel.ui.theme.AppType
import com.jekael.adoel.ui.theme.Blue400
import com.jekael.adoel.ui.theme.Emerald500
import com.jekael.adoel.ui.theme.LocalAppColors
import com.jekael.adoel.ui.theme.Red400
import com.jekael.adoel.ui.theme.elevatedListCard

/** Dashboard-style stat row (Mesin Aktif / Sudah Doff / Terlambat) shown above the
 * Estimasi/Aktual list content, regardless of which mode is active. */
internal fun LazyListScope.statSummarySection(mesinAktif: Int, sudahDoff: Int, terlambat: Int) {
    item(key = "stat_summary") {
        Row(
            modifier = Modifier.fillMaxWidth().animateItem(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile(label = "Mesin Aktif", count = mesinAktif, valueColor = Blue400, modifier = Modifier.weight(1f))
            StatTile(label = "Sudah Doff", count = sudahDoff, valueColor = Emerald500, modifier = Modifier.weight(1f))
            StatTile(label = "Terlambat", count = terlambat, valueColor = Red400, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(label: String, count: Int, valueColor: Color, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .elevatedListCard(backgroundColor = colors.bgElevated)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(count.toString(), style = AppType.NumberLarge.copy(color = valueColor))
        Text(label, style = AppType.Caption.copy(color = colors.textFaint))
    }
}
