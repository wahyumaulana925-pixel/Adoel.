package com.jekael.adoel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActionSheet(
    onClose: () -> Unit,
    onCatatEstimasi: () -> Unit,
    onCatatDoffing: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Zinc950,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Zinc700),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = "Mau catat apa?",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Zinc100),
                modifier = Modifier.padding(bottom = 16.dp),
            )

            AddActionOption(
                title = "Catat Estimasi",
                subtitle = "Perkirakan kapan mesin siap doff",
                icon = { Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Amber400, modifier = Modifier.size(28.dp)) },
                accentBg = Amber500.copy(alpha = 0.12f),
                onClick = { onClose(); onCatatEstimasi() },
            )

            Spacer(Modifier.height(12.dp))

            AddActionOption(
                title = "Catat Doffing",
                subtitle = "Catat mesin yang baru selesai diambil",
                icon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Cyan400, modifier = Modifier.size(28.dp)) },
                accentBg = Cyan500.copy(alpha = 0.12f),
                onClick = { onClose(); onCatatDoffing() },
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AddActionOption(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    accentBg: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Zinc900)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentBg),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Zinc100))
            Text(subtitle, style = TextStyle(fontSize = 12.5.sp, color = Zinc500))
        }
    }
}
