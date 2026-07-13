package com.jekael.adoel.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jekael.adoel.data.MesinTipe
import com.jekael.adoel.ui.theme.Amber500
import com.jekael.adoel.ui.theme.LocalAppColors
import com.jekael.adoel.ui.theme.Sky500
import com.jekael.adoel.ui.theme.Teal500
import com.jekael.adoel.ui.theme.Violet500

/** One color per [MesinTipe], reused everywhere a machine's type needs a visual tag (RadarCard,
 * DoffingSection's row dot, Pengaturan > Mesin, Statistik breakdown) — a single source so the
 * "color of a machine type" language can't drift between screens. */
fun mesinTipeColor(tipe: MesinTipe?): Color = when (tipe) {
    MesinTipe.TAPPET -> Teal500
    MesinTipe.CAM -> Violet500
    MesinTipe.D405 -> Amber500
    MesinTipe.D408 -> Sky500
    null -> Color.Gray
}

/** One icon per [MesinTipe] — purely a visual identifier alongside the type label/color, no
 * semantic meaning beyond "this is a different kind of machine at a glance". */
fun mesinTipeIcon(tipe: MesinTipe): ImageVector = when (tipe) {
    MesinTipe.TAPPET -> Icons.Outlined.Build
    MesinTipe.CAM -> Icons.Outlined.Circle
    MesinTipe.D405 -> Icons.Outlined.Speed
    MesinTipe.D408 -> Icons.Outlined.Numbers
}

@Composable
fun CheckIcon() {
    Icon(
        imageVector = Icons.Outlined.Check,
        contentDescription = "Doff",
        modifier = Modifier.size(20.dp),
    )
}

@Composable
fun TrashIcon(size: Dp = 18.dp) {
    Icon(
        imageVector = Icons.Outlined.Delete,
        contentDescription = "Hapus",
        modifier = Modifier.size(size),
    )
}

@Composable
fun GearIcon() {
    Icon(
        imageVector = Icons.Outlined.Settings,
        contentDescription = "Pengaturan",
        tint = LocalAppColors.current.textPrimary,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
fun SendIcon() {
    Icon(
        imageVector = Icons.Outlined.Send,
        contentDescription = "Send",
        modifier = Modifier.size(20.dp),
    )
}

@Composable
fun CloseIcon() {
    Icon(
        imageVector = Icons.Outlined.Close,
        contentDescription = "Tutup",
        tint = LocalAppColors.current.textMuted,
        modifier = Modifier.size(18.dp),
    )
}
