package com.jekael.adoel.ui.components

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.AktualEntry
import com.jekael.adoel.data.DoffState
import com.jekael.adoel.data.absMinToTimeStr
import com.jekael.adoel.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDrawer(
    state: DoffState,
    onClose: () -> Unit,
    onEditAkt: (Int) -> Unit,
    onHapus: (AktualEntry) -> Unit,
    onShare: () -> Unit,
    onFinishShift: () -> Unit,
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
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Zinc100),
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
                    style = TextStyle(fontSize = 14.sp, color = Zinc600),
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
                        onHapus = { onHapus(entry) },
                    )
                }
            }
        }

        // Footer
        HorizontalDivider(color = Zinc800, modifier = Modifier.padding(top = 12.dp))
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Zinc400),
                border = BorderStroke(1.dp, Zinc700),
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

private const val SWIPE_SENSITIVITY = 1.8f
private const val SNAP_OPEN_FRACTION = 0.32f

@Composable
private fun HistoryRow(
    entry: AktualEntry,
    index: Int,
    total: Int,
    mesinDb: Map<String, com.jekael.adoel.data.MesinData>,
    onEdit: () -> Unit,
    onHapus: () -> Unit,
) {
    val mesin = mesinDb[entry.mcNo]
    val corak = entry.corakOverride ?: mesin?.corak ?: "—"
    val num = total - index

    val revealDp = 150.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val revealPx = with(density) { revealDp.toPx() }
    val animOffset = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val currentOffset = if (isDragging) dragOffset else animOffset.value
    val scope = rememberCoroutineScope()

    fun snapTo(open: Boolean) = scope.launch {
        animOffset.animateTo(
            if (open) revealPx else 0f,
            animationSpec = tween(220, easing = FastOutSlowInEasing),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Zinc900),
    ) {
        // Action buttons
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(revealDp)
                .fillMaxHeight(),
        ) {
            Button(
                onClick = { snapTo(false); onEdit() },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan700),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("EDIT", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(Zinc950.copy(alpha = 0.2f)))
            Button(
                onClick = { snapTo(false); onHapus() },
                modifier = Modifier.width(65.dp).fillMaxHeight(),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Red700),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("HAPUS", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp))
            }
        }

        // Row face
        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = -currentOffset }
                .background(Zinc900)
                .padding(horizontal = 16.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        isDragging = false
                        val startOffset = animOffset.value
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                if (isDragging) {
                                    isDragging = false
                                    val open = dragOffset > revealPx * SNAP_OPEN_FRACTION
                                    scope.launch {
                                        animOffset.snapTo(dragOffset)
                                        animOffset.animateTo(
                                            if (open) revealPx else 0f,
                                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                                        )
                                    }
                                }
                                break
                            }
                            val dx = down.position.x - change.position.x
                            if (!isDragging && abs(dx) > 8f) isDragging = true
                            if (isDragging) {
                                change.consume()
                                dragOffset = (startOffset + dx * SWIPE_SENSITIVITY).coerceIn(0f, revealPx)
                            }
                        }
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Sequence number
            Text(
                text = "$num",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Zinc500,
                ),
                modifier = Modifier.width(20.dp),
            )
            // Type dot
            val dotColor = when (mesin?.tipe) {
                com.jekael.adoel.data.MesinTipe.TAPPET -> Teal500
                com.jekael.adoel.data.MesinTipe.CAM -> Violet500
                com.jekael.adoel.data.MesinTipe.D405 -> Amber500
                com.jekael.adoel.data.MesinTipe.D408 -> Sky500
                null -> Zinc600
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
                        fontSize = 28.sp,
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
                        color = Zinc500,
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
                    color = Zinc200,
                ),
            )
        }
    }
}
