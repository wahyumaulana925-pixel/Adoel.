package com.jekael.adoel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.AktualEntry
import com.jekael.adoel.data.DoffState
import com.jekael.adoel.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDrawer(
    state: DoffState,
    onClose: () -> Unit,
    onEditAkt: (Int) -> Unit,
    onShare: () -> Unit,
    onFinishShift: () -> Unit,
) {
    val colors = LocalAppColors.current
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = colors.bg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.border),
            )
        },
    ) {
        val chronological = remember(state.aktual) { state.aktual.toList() }
        val total = chronological.size

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Riwayat",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary),
            )
            Text(
                text = "$total doff",
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Cyan500),
            )
        }

        Spacer(Modifier.height(8.dp))

        // List
        if (total == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Belum ada doff",
                    style = TextStyle(fontSize = 14.sp, color = colors.textFaint),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(chronological, key = { _, e -> e.id }) { idx, entry ->
                    HistoryRow(
                        entry = entry,
                        index = idx,
                        total = total,
                        mesinDb = state.db,
                        onEdit = { onEditAkt(entry.id) },
                    )
                }
            }
        }

        // Footer
        HorizontalDivider(color = colors.border, modifier = Modifier.padding(top = 12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onShare,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                border = BorderStroke(1.dp, colors.border),
            ) {
                Text("Bagikan", style = TextStyle(fontSize = 14.sp))
            }
            Button(
                onClick = onFinishShift,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
            ) {
                Text("Selesai Shift", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: AktualEntry,
    index: Int,
    total: Int,
    mesinDb: Map<String, com.jekael.adoel.data.MesinData>,
    onEdit: () -> Unit,
) {
    val colors = LocalAppColors.current
    val mesin = mesinDb[entry.mcNo]
    val corak = entry.corakOverride ?: mesin?.corak ?: "—"
    val num = total - index

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgElevated)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Sequence number
        Text(
            text = "$num",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = colors.textMuted,
            ),
            modifier = Modifier.width(20.dp),
        )
        // Type dot
        val dotColor = when (mesin?.tipe) {
            com.jekael.adoel.data.MesinTipe.TAPPET -> Teal500
            com.jekael.adoel.data.MesinTipe.CAM -> Violet500
            com.jekael.adoel.data.MesinTipe.D405 -> Amber500
            com.jekael.adoel.data.MesinTipe.D408 -> Sky500
            null -> colors.textFaint
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        // Machine + corak
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.mcNo,
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    color = Cyan500,
                ),
            )
            val sub = if (entry.customYard != null) "$corak · ${entry.customYard}y" else corak
            Text(
                text = sub,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = colors.textMuted,
                ),
                maxLines = 1,
            )
        }
        // Ket
        Text(
            text = entry.ket,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            ),
        )
        // Action button — always visible, no swipe
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(32.dp).background(Cyan700, CircleShape),
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
