package com.jekael.adoel.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.ui.theme.AppType
import com.jekael.adoel.ui.theme.Cyan600
import com.jekael.adoel.ui.theme.LocalAppColors

/**
 * Light first-run explainer — also reachable anytime via Pengaturan > Data > Bantuan, so the
 * same content is written once and shared between the auto-shown and on-demand entry points.
 */
@Composable
fun OnboardingDialog(onClose: () -> Unit) {
    val colors = LocalAppColors.current
    FloatingEditDialog(onDismissRequest = onClose) {
        Text("Cara Pakai Adoel", style = AppType.DialogTitle.copy(color = colors.textPrimary))
        Spacer(Modifier.height(16.dp))

        val bullets = listOf(
            "ESTIMASI: catat kapan mesin akan selesai — nomor mesin + durasi/yard/jam counter, tergantung tipe mesin.",
            "DOFFING: catat mesin yang sudah selesai — nomor mesin + keterangan bebas, mis. \"31 HB\".",
            "Corak = kode motif yang sedang dikerjakan mesin, diatur di Pengaturan > Mesin.",
            "Contoh perintah ESTIMASI: TAPPET/CAM \"31 45\" (sisa menit) · D405 \"31 280\" (yard berjalan) · D408 \"31 12.30\" (jam counter).",
        )
        bullets.forEach {
            Text(
                "•  $it",
                style = AppType.BodySmall.copy(color = colors.textSecondary, lineHeight = 18.sp),
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
        ) { Text("Mengerti", fontWeight = FontWeight.SemiBold) }
    }
}
