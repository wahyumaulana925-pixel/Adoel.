package com.jekael.adoel.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.DEFAULT_CORAK_SHORTCUTS
import com.jekael.adoel.data.DEFAULT_KETERANGAN_SHORTCUTS
import com.jekael.adoel.ui.theme.*

/**
 * Reusable shortcut chip selector and quick inline adder for Corak and Keterangan.
 * Aligns 1:1 with Web's ShortcutPicker component.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BaseShortcutPicker(
    value: String,
    onSelect: (String) -> Unit,
    shortcuts: List<String>,
    onAddShortcut: (String) -> Unit,
    itemTypeLabel: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    showToast: ((String) -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    var isAdding by remember { mutableStateOf(false) }
    var inlineInput by remember { mutableStateOf("") }

    val currentTrimmed = remember(value) { value.trim().uppercase() }
    val canSaveCurrent = remember(currentTrimmed, shortcuts) {
        currentTrimmed.isNotEmpty() && shortcuts.none { it.equals(currentTrimmed, ignoreCase = true) }
    }

    fun handleSaveCurrent() {
        if (currentTrimmed.isEmpty()) return
        onAddShortcut(currentTrimmed)
        showToast?.invoke("$itemTypeLabel \"$currentTrimmed\" ditambahkan ke shortcut ✓")
    }

    fun handleAddInline() {
        val trimmed = inlineInput.trim().uppercase()
        if (trimmed.isEmpty()) return
        if (shortcuts.any { it.equals(trimmed, ignoreCase = true) }) {
            showToast?.invoke("$itemTypeLabel \"$trimmed\" sudah ada di shortcut")
        } else {
            onAddShortcut(trimmed)
            showToast?.invoke("$itemTypeLabel \"$trimmed\" ditambahkan ke shortcut ✓")
        }
        onSelect(trimmed)
        inlineInput = ""
        isAdding = false
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Existing shortcut chips
            shortcuts.forEach { code ->
                val isActive = currentTrimmed.equals(code, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isActive) Cyan600.copy(alpha = 0.22f) else colors.bgElevated2
                        )
                        .clickable { onSelect(code) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = code,
                        style = AppType.Caption.copy(
                            color = if (isActive) Cyan400 else colors.textSecondary,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                        ),
                    )
                }
            }

            // Quick save current value button
            if (canSaveCurrent) {
                Surface(
                    onClick = { handleSaveCurrent() },
                    shape = RoundedCornerShape(6.dp),
                    color = Cyan600.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Cyan600.copy(alpha = 0.35f)),
                    modifier = Modifier.height(26.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "+ Simpan \"$currentTrimmed\" ke Shortcut",
                            style = AppType.Caption.copy(
                                color = Cyan400,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }
            }

            // Inline Add Trigger Button
            if (!isAdding) {
                Surface(
                    onClick = { isAdding = true },
                    shape = RoundedCornerShape(6.dp),
                    color = colors.bgElevated1,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.height(26.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "+ Tambah $itemTypeLabel",
                            style = AppType.Caption.copy(
                                color = colors.textMuted,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }
            }
        }

        // Inline Add Input Bar
        AnimatedVisibility(
            visible = isAdding,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.RadiusControl))
                    .background(colors.bgElevated2)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = inlineInput,
                    onValueChange = { inlineInput = it.uppercase() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    placeholder = {
                        Text(
                            placeholder,
                            style = AppType.Caption.copy(color = colors.textFaint, fontSize = 11.sp),
                        )
                    },
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(6.dp),
                    textStyle = AppType.FieldText.copy(fontSize = 12.sp, color = colors.textPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { handleAddInline() }),
                    singleLine = true,
                )
                Button(
                    onClick = { handleAddInline() },
                    enabled = inlineInput.trim().isNotEmpty(),
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cyan600,
                        disabledContainerColor = colors.bgElevated1,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) {
                    Text(
                        "+ OK",
                        style = AppType.Caption.copy(
                            color = if (inlineInput.trim().isNotEmpty()) Color.White else colors.textFaint,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                    )
                }
                IconButton(
                    onClick = { isAdding = false },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Batal",
                        tint = colors.textFaint,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun CorakShortcutPicker(
    value: String,
    onSelect: (String) -> Unit,
    shortcuts: List<String>?,
    onAddShortcut: (String) -> Unit,
    modifier: Modifier = Modifier,
    showToast: ((String) -> Unit)? = null,
) {
    BaseShortcutPicker(
        value = value,
        onSelect = onSelect,
        shortcuts = shortcuts ?: DEFAULT_CORAK_SHORTCUTS,
        onAddShortcut = onAddShortcut,
        itemTypeLabel = "Corak",
        placeholder = "Cth: 4520",
        modifier = modifier,
        showToast = showToast,
    )
}

@Composable
fun KeteranganShortcutPicker(
    value: String,
    onSelect: (String) -> Unit,
    shortcuts: List<String>?,
    onAddShortcut: (String) -> Unit,
    modifier: Modifier = Modifier,
    showToast: ((String) -> Unit)? = null,
) {
    BaseShortcutPicker(
        value = value,
        onSelect = onSelect,
        shortcuts = shortcuts ?: DEFAULT_KETERANGAN_SHORTCUTS,
        onAddShortcut = onAddShortcut,
        itemTypeLabel = "Keterangan",
        placeholder = "Cth: GANTI BEAM",
        modifier = modifier,
        showToast = showToast,
    )
}
