package com.jekael.adoel.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.ui.theme.AppType
import com.jekael.adoel.ui.theme.Cyan600
import com.jekael.adoel.ui.theme.Dimens
import com.jekael.adoel.ui.theme.LocalAppColors

/**
 * Light first-run explainer — also reachable anytime via Pengaturan > Data > Bantuan, so the
 * same content is written once and shared between the auto-shown and on-demand entry points.
 */
@Composable
fun OnboardingDialog(onClose: () -> Unit) {
    val colors = LocalAppColors.current
    FloatingEditDialog(onDismissRequest = onClose) {
        Text("Panduan Penggunaan", style = AppType.DialogTitle.copy(color = colors.textPrimary))
        Spacer(Modifier.height(Dimens.Space16))

        var tab by remember { mutableIntStateOf(0) }
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Input Data Mesin") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Simulasi 5 Gestur") })
        }
        Spacer(Modifier.height(Dimens.Space16))

        val bullets = listOf(
            "ESTIMASI (⏱): Ketik nomor mesin di konsol bawah, ketuk ikon jam, lalu isi sisa menit (Tappet/Cam), yard berjalan (D405), atau jam counter (D408) sesuai petunjuk di layar.",
            "DOFFING / POTONG KAIN (✂): Cara cepat — geser kartu mesin di layar Radar ke kanan (Potong Normal) atau ke kiri (Potong Matching). Cara langsung — ketik nomor mesin di konsol bawah, ketuk ikon gunting, lalu pilih tindakan.",
            "URUNGKAN & ULANG (↩ / ↪): Salah mencatat atau salah hapus? Tombol Undo/Redo di kiri konsol bawah mengembalikan data secara instan.",
            "KENDALA MESIN & MACET: Jika mesin berhenti/macet (mis. putus lusi), hapus estimasinya (tekan lama kartu radar lalu pilih Hapus) agar perhitungan waktu JEDA istirahat tetap akurat.",
        )
        if (tab == 0) bullets.forEach {
            Text(
                "•  $it",
                style = AppType.BodySmall.copy(color = colors.textSecondary, lineHeight = 18.sp),
            )
            Spacer(Modifier.height(10.dp))
        }

        if (tab == 1) {
            var selectedGesture by remember { mutableIntStateOf(0) }
            val gestures = listOf(
                "Geser kanan" to "Doffing normal",
                "Geser kiri" to "Doffing matching",
                "Tekan lama" to "Buka aksi kartu",
                "Ketuk waktu" to "Edit estimasi",
                "Ketuk nomor/corak" to "Edit data mesin",
            )
            gestures.forEachIndexed { index, (gesture, action) ->
                Button(
                    onClick = { selectedGesture = index },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedGesture == index) Cyan600 else colors.bgElevated2),
                ) { Text("$gesture  ->  $action") }
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.RadiusControl),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
        ) { Text("Mengerti", fontWeight = FontWeight.SemiBold) }
    }
}
