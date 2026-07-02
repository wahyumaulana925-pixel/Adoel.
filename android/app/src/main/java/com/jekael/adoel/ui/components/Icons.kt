package com.jekael.adoel.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
fun TrashIcon() {
    Icon(
        imageVector = Icons.Outlined.Delete,
        contentDescription = "Hapus",
        modifier = Modifier.size(18.dp),
    )
}

@Composable
fun GearIcon() {
    Icon(
        imageVector = Icons.Outlined.Menu,
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
fun HistoryIcon() {
    Icon(
        imageVector = Icons.Outlined.List,
        contentDescription = "Riwayat",
        modifier = Modifier.size(22.dp),
    )
}
