package com.jekael.adoel.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.ui.theme.AppType
import com.jekael.adoel.ui.theme.LocalAppColors

/** The three dismissible permission/battery nudges shown above the console list — notification
 * permission, exact-alarm scheduling, and battery-optimization exemption — each only relevant
 * once the one before it is already granted, so at most one shows at a time. */
fun LazyListScope.permissionBanners(
    notifGranted: Boolean,
    exactAlarmGranted: Boolean,
    batteryUnrestricted: Boolean,
    onNotifBannerClick: () -> Unit,
    onExactAlarmBannerClick: () -> Unit,
    onBatteryBannerClick: () -> Unit,
) {
    if (!notifGranted) {
        item {
            val colors = LocalAppColors.current
            TextButton(
                onClick = onNotifBannerClick,
                modifier = Modifier.fillMaxWidth().animateItem(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = colors.bannerWarnBg,
                    contentColor = colors.bannerWarnFg,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text("Notifikasi nonaktif — ketuk untuk izinkan", style = AppType.Caption)
            }
        }
    }

    if (notifGranted && !exactAlarmGranted) {
        item {
            val colors = LocalAppColors.current
            TextButton(
                onClick = onExactAlarmBannerClick,
                modifier = Modifier.fillMaxWidth().animateItem(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = colors.bannerWarnBg,
                    contentColor = colors.bannerWarnFg,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    "Izin alarm tepat waktu nonaktif — ketuk untuk izinkan (wajib agar notifikasi doff tepat waktu)",
                    style = AppType.Caption,
                )
            }
        }
    }

    if (notifGranted && exactAlarmGranted && !batteryUnrestricted) {
        item {
            val colors = LocalAppColors.current
            TextButton(
                onClick = onBatteryBannerClick,
                modifier = Modifier.fillMaxWidth().animateItem(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = colors.bannerWarnBg,
                    contentColor = colors.bannerWarnFg,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text("Baterai dioptimalkan — ketuk agar notifikasi tidak diblokir sistem", style = AppType.Caption)
            }
        }
    }
}

/** Warns when the console still holds doff/estimasi entries from a shift period that's already
 * over (operator forgot to tap "Selesai Shift" before the next 06.00/14.00/22.00 boundary) —
 * shown regardless of ESTIMASI/DOFFING mode so it can't be missed by only checking one tab, and
 * left visible (not auto-archived) since the operator may still be copying the old list into a
 * written report. Left off entirely once there's nothing stale to flag. */
fun LazyListScope.staleShiftBanner(staleCount: Int, onFinishClick: () -> Unit) {
    if (staleCount <= 0) return
    item {
        val colors = LocalAppColors.current
        TextButton(
            onClick = onFinishClick,
            modifier = Modifier.fillMaxWidth().animateItem(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.textButtonColors(
                containerColor = colors.bannerWarnBg,
                contentColor = colors.bannerWarnFg,
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                "Ada $staleCount catatan dari shift sebelumnya yang belum diarsipkan — ketuk untuk Selesai Shift dulu",
                style = AppType.Caption,
            )
        }
    }
}
