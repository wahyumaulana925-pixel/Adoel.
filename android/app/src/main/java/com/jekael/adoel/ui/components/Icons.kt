package com.jekael.adoel.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        imageVector = Icons.Outlined.Settings,
        contentDescription = "Settings",
        modifier = Modifier.size(18.dp),
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

@Composable
fun ActionButton(
    label: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            icon()
            Text(
                text = label,
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            )
        }
    }
}
