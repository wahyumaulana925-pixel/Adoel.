package com.jekael.adoel.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jekael.adoel.data.*
import com.jekael.adoel.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class SettingsTab { MESIN, DATA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDrawer(
    state: DoffState,
    onClose: () -> Unit,
    onSetMesin: (String, MesinData) -> Unit,
    onResetMesin: (String) -> Unit,
    onResetDb: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onExportJson: () -> String,
    onImport: (String) -> Unit,
    showToast: (String) -> Unit,
    showConfirm: (String, () -> Unit) -> Unit,
) {
    var tab by remember { mutableStateOf(SettingsTab.MESIN) }
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()

    // Manual drag offset for swipe-right-to-dismiss; separate from SlidePanel's own enter/exit
    // transition so the two animation systems never fight over the same value. On a drag past
    // threshold this calls onClose directly (bypassing SlidePanel's requestClose/exit-animation
    // dance below) since the drag itself already provides the visual exit.
    val dragOffset = remember { Animatable(0f) }
    var panelWidthPx by remember { mutableStateOf(0f) }

    var headerHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    SlidePanel(onClose = onClose) { requestClose ->
        // Box, not Column: the header floats as an overlay on top of the tab content (which is
        // laid out full-size from the very top) so the content actually scrolls behind it,
        // matching MainScreen's header/console concept instead of just sitting above it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
                .onGloballyPositioned { panelWidthPx = it.size.width.toFloat() }
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val width = if (panelWidthPx > 0f) panelWidthPx else 1f
                            if (dragOffset.value > width * 0.3f) {
                                scope.launch {
                                    dragOffset.animateTo(width, animationSpec = tween(200))
                                    onClose()
                                }
                            } else {
                                scope.launch {
                                    dragOffset.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { dragOffset.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        val newVal = (dragOffset.value + dragAmount).coerceAtLeast(0f)
                        scope.launch { dragOffset.snapTo(newVal) }
                    }
                },
            // No systemBarsPadding() here — this panel now lives inside MainScreen's own root
            // Box, which already insets its children from the system bars once.
        ) {
            AnimatedContent(
                targetState = tab,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(animationSpec = tween(220)) { w -> dir * w } + fadeIn(tween(180)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(220)) { w -> -dir * w } + fadeOut(tween(140)))
                },
                label = "settingsTabContent",
            ) { t ->
                when (t) {
                    SettingsTab.MESIN -> MesinTab(state, headerHeight, onSetMesin, onResetMesin, showToast, showConfirm)
                    SettingsTab.DATA -> DataTab(state, headerHeight, onResetDb, onSetThemeMode, onExportJson, onImport, showToast, showConfirm)
                }
            }

            // Header + tab switcher — floating overlay, matching the header/console bar's look
            // (shadow + rounded corners + a subtle border, since shadows alone barely read on a
            // near-black dark background).
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        headerHeight = with(density) { coords.size.height.toDp() }
                    }
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp)
                    .floatingHeaderCard(),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Pengaturan", style = AppType.DialogTitle.copy(color = colors.textPrimary))
                        IconButton(onClick = { requestClose() }) {
                            CloseIcon()
                        }
                    }
                    SlidingToggle(
                        labelLeft = "Mesin",
                        labelRight = "Data",
                        selectedIndex = if (tab == SettingsTab.MESIN) 0 else 1,
                        onSelect = { tab = if (it == 0) SettingsTab.MESIN else SettingsTab.DATA },
                        containerColor = colors.bgElevated2,
                        activeColorLeft = Cyan600,
                        activeColorRight = Cyan600,
                        activeTextColorLeft = Zinc950,
                        activeTextColorRight = Zinc950,
                        inactiveTextColor = colors.textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 14.dp),
                    )
                }
            }
        }
    }
}
