package com.jekael.adoel.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jekael.adoel.ui.theme.LocalAppColors

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
