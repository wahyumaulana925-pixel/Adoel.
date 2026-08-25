package com.jekael.adoel.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Forward
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.ui.theme.AppType
import com.jekael.adoel.ui.theme.Amber400
import com.jekael.adoel.ui.theme.Amber500
import com.jekael.adoel.ui.theme.Amber700
import com.jekael.adoel.ui.theme.Cyan600
import com.jekael.adoel.ui.theme.Dimens
import com.jekael.adoel.ui.theme.Emerald500
import com.jekael.adoel.ui.theme.LocalAppColors
import com.jekael.adoel.ui.theme.WovenDivider
import com.jekael.adoel.ui.theme.Zinc500

/**
 * Light first-run explainer — also reachable anytime via Pengaturan > Data > Bantuan, so the
 * same content is written once and shared between the auto-shown and on-demand entry points.
 */
    @Composable
    fun OnboardingDialog(onClose: () -> Unit) {
        val colors = LocalAppColors.current
        var tab by remember { mutableIntStateOf(0) }
        FloatingEditDialog(onDismissRequest = onClose) {
            Text("Cara Pakai Adoel", style = AppType.DialogTitle.copy(color = colors.textPrimary))
            Spacer(Modifier.height(Dimens.Space12))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
            ) {
                listOf("Panduan", "Simulasi Gestur").forEachIndexed { index, label ->
                    FilterChip(selected = tab == index, onClick = { tab = index }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(Dimens.Space12))
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space12),
            ) {
                if (tab == 0) {
                    GuideRow(Icons.Outlined.Schedule, Cyan600, "ESTIMASI", "Pilih tipe mesin, isi corak dan target yard. D405 meminta speed, sedangkan D408 meminta koreksi dan menyediakan kalkulator.")
                    WovenDivider()
                    GuideRow(Icons.Outlined.ContentCut, Emerald500, "DOFFING", "Geser kartu ke kanan untuk potong normal atau ke kiri untuk potong matching.")
                    WovenDivider()
                    GuideRow(Icons.Outlined.Pause, Amber500, "JEDA", "Kartu yang dijeda menampilkan nomor Mc, corak, yard, dan sisa waktu yang dibekukan saat Jeda ditekan.")
                    WovenDivider()
                    GuideRow(Icons.Outlined.Add, Zinc500, "KALKULATOR", "Gunakan tombol - dan + di samping Koreksi untuk mengurangi atau menambah satu menit.")
                    WovenDivider()
                    GuideRow(Icons.Outlined.Undo, Amber700, "UNDO", "Koreksi kesalahan secara instan melalui tombol Undo di konsol.")
                    WovenDivider()
                    GuideRow(Icons.Outlined.Forward, Amber400, "OPERAN", "Estimasi setelah batas 8 jam dipisahkan sebagai operan shift berikutnya.")
                } else {
                    GestureLegend()
                }
            }
            Spacer(Modifier.height(Dimens.Space16))
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
            ) { Text("Mengerti", fontWeight = FontWeight.SemiBold) }
        }
    }

@Composable
private fun GuideRow(icon: ImageVector, tint: Color, title: String, description: String) {
    val colors = LocalAppColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space12), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space4)) {
            Text(title, style = AppType.LabelSmallBold.copy(color = tint))
            Text(description, style = AppType.BodySmall.copy(color = colors.textSecondary, lineHeight = 18.sp))
        }
    }
}

@Composable
private fun GestureLegend() {
    val colors = LocalAppColors.current
    Text("Simulasi Gestur", style = AppType.LabelSmallBold.copy(color = colors.textPrimary))
    Box(
        modifier = Modifier.fillMaxWidth().height(118.dp).background(colors.bgElevated2, RoundedCornerShape(Dimens.RadiusControl)).padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("←", style = AppType.NumberLarge.copy(color = Amber400))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.ContentCut, contentDescription = null, tint = Emerald500, modifier = Modifier.size(28.dp))
                Text("Mc 12", style = AppType.LabelBold.copy(color = colors.textPrimary))
                Text("⟷", style = AppType.Caption.copy(color = colors.textFaint))
            }
            Text("→", style = AppType.NumberLarge.copy(color = Cyan600))
        }
    }
    Spacer(Modifier.height(Dimens.Space8))
    GestureLegendRow("Tap kiri", "Edit mesin", Amber400)
    GestureLegendRow("Tap kanan", "Edit waktu", Cyan600)
    GestureLegendRow("Swipe", "Doffing", Emerald500)
}

@Composable
private fun GestureLegendRow(action: String, meaning: String, tint: Color) {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(action, style = AppType.LabelSmallBold.copy(color = tint))
        Text(meaning, style = AppType.BodySmall.copy(color = colors.textSecondary))
    }
}
