package com.jekael.adoel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Forward
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space12),
        ) {
            Text("Cara Pakai Adoel", style = AppType.DialogTitle.copy(color = colors.textPrimary))
            SlidingToggle(
                labelLeft = "Instruksi", labelRight = "Simulasi", selectedIndex = tab,
                onSelect = { tab = it }, containerColor = colors.bgElevated2,
                activeColorLeft = Cyan600, activeColorRight = Emerald500,
                activeTextColorLeft = Color.White, activeTextColorRight = Color.White,
                inactiveTextColor = colors.textMuted, modifier = Modifier.fillMaxWidth(),
                accessibilityLabel = "Mode panduan Adoel",
            )
            if (tab == 0) Instructions() else GestureLegend()
            Button(
                onClick = onClose, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
            ) { Text("Mengerti", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun Instructions() {
    GuideRow(Icons.Outlined.Schedule, Cyan600, "ESTIMASI", "Input sisa waktu, yard berjalan, atau jam counter. Kini kamu bisa Full Setup (Tipe/Speed/Koreksi) langsung di sini.")
    WovenDivider()
    GuideRow(Icons.Outlined.ContentCut, Emerald500, "POTONG / DOFFING", "Geser kartu ke kanan (Normal) atau kiri (Matching) untuk mencatat. Bisa juga klik ikon gunting di konsol.")
    WovenDivider()
    GuideRow(Icons.Outlined.Pause, Amber500, "JEDA MESIN", "Tekan lama kartu untuk Jeda. Sisi belakang kartu sekarang tetap menampilkan info Corak & Yard yang dibekukan.")
    WovenDivider()
    GuideRow(Icons.Outlined.Add, Zinc500, "KALKULATOR", "Gunakan tombol [+] dan [-] untuk koreksi menit D408 secara instan tanpa mengetik ulang.")
    WovenDivider()
    GuideRow(Icons.Outlined.Undo, Amber700, "URUNGKAN (UNDO)", "Salah tekan? Gunakan tombol Undo/Redo di kiri konsol untuk membatalkan aksi terakhir.")
    WovenDivider()
    GuideRow(Icons.Outlined.Forward, Amber400, "OPERAN SHIFT", "Mesin yang doff lebih dari 8 jam ke depan otomatis ditandai sebagai Operan agar progres bar tetap akurat.")
}

@Composable
private fun GuideRow(icon: ImageVector, tint: Color, title: String, description: String) {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.Space12), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(50)).background(tint), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space4)) {
            Text(title, style = AppType.LabelBold.copy(color = tint))
            Text(description, style = AppType.BodySmall.copy(color = colors.textSecondary, lineHeight = 18.sp))
        }
    }
}

@Composable
private fun GestureLegend() {
    val colors = LocalAppColors.current
    Text("Practice Area", style = AppType.LabelBold.copy(color = colors.textPrimary))
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = (maxWidth * 0.48f).coerceAtLeast(150.dp)
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
            Box(modifier = Modifier.fillMaxWidth()) { GestureCallout(Icons.Outlined.ArrowBack, "Ketuk: Edit Mesin", Amber400, Alignment.Start) }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MiniRadarCard(Modifier.width(cardWidth)) }
            Box(modifier = Modifier.fillMaxWidth()) { GestureCallout(Icons.Outlined.ArrowForward, "Ketuk: Edit Waktu", Cyan600, Alignment.End) }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                GestureCallout(Icons.Outlined.ArrowBack, "Geser: Potong Kain", Emerald500, Alignment.Start, Modifier.weight(1f))
                GestureCallout(Icons.Outlined.TouchApp, "Tekan Lama: Jeda / Hapus", Zinc500, Alignment.End, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GestureCallout(icon: ImageVector, label: String, tint: Color, alignment: Alignment.Horizontal, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(modifier = modifier, horizontalArrangement = if (alignment == Alignment.End) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
        if (alignment == Alignment.End) Text(label, style = AppType.BodySmall.copy(color = colors.textSecondary))
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.padding(horizontal = 4.dp).size(20.dp))
        if (alignment == Alignment.Start) Text(label, style = AppType.BodySmall.copy(color = colors.textSecondary))
    }
}

@Composable
private fun MiniRadarCard(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(modifier = modifier.background(colors.bgElevated2, RoundedCornerShape(Dimens.RadiusControl)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MC 12", style = AppType.LabelBold.copy(color = colors.textPrimary))
            Icon(Icons.Outlined.Schedule, contentDescription = "Sisa waktu", tint = Cyan600, modifier = Modifier.size(18.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("D408", style = AppType.BodySmall.copy(color = colors.textSecondary))
            Text("02j 40m", style = AppType.LabelBold.copy(color = Amber500))
        }
        LinearProgressIndicator(progress = { 0.62f }, modifier = Modifier.fillMaxWidth(), color = Cyan600, trackColor = colors.border)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Corak A", style = AppType.Caption.copy(color = colors.textMuted))
            Icon(Icons.Outlined.ContentCut, contentDescription = "Potong kain", tint = Emerald500, modifier = Modifier.size(18.dp))
        }
    }
}
