package com.jekael.adoel.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jekael.adoel.data.*
import com.jekael.adoel.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DataTab(
    state: DoffState,
    headerHeight: Dp,
    onResetDb: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onExportJson: () -> String,
    onImport: (String) -> Unit,
    showToast: (String) -> Unit,
    showConfirm: (String, () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val currentTheme = remember(state.themeMode) {
        runCatching { ThemeMode.valueOf(state.themeMode) }.getOrDefault(ThemeMode.SYSTEM)
    }

    // Backup: user picks where to save a .json file; we write the full-state JSON to it.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(onExportJson().toByteArray()) }
                }.isSuccess
            }
            showToast(if (ok) "Data dicadangkan ✓" else "⚠ Gagal menyimpan file")
        }
    }

    // Restore: user picks a backup file; we read its text and hand it to onImport (which confirms).
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (text != null) onImport(text) else showToast("⚠ Gagal membaca file")
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Scrolls behind the floating header/tab-switcher card above instead of being pushed
        // down by it — see the Box-overlay comment on SettingsDrawer's root.
        Spacer(Modifier.height(10.dp + headerHeight + Dimens.Space16))
        FieldLabel("Tema Aplikasi")
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
            ChipBtn("Sistem", currentTheme == ThemeMode.SYSTEM) { onSetThemeMode(ThemeMode.SYSTEM) }
            ChipBtn("Gelap", currentTheme == ThemeMode.DARK) { onSetThemeMode(ThemeMode.DARK) }
            ChipBtn("Terang", currentTheme == ThemeMode.LIGHT) { onSetThemeMode(ThemeMode.LIGHT) }
        }

        Spacer(Modifier.height(Dimens.Space4))
        WovenDivider()
        Spacer(Modifier.height(Dimens.Space4))

        FieldLabel("Cadangan Data")
        Text(
            "Cadangkan seluruh data (mesin, estimasi, riwayat doff, tema) ke file, atau pulihkan dari file cadangan.",
            style = AppType.Caption.copy(color = colors.textMuted),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
            OutlinedButton(
                onClick = {
                    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                    runCatching { exportLauncher.launch("adoel-backup-$stamp.json") }
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                border = BorderStroke(1.dp, colors.border),
            ) { Text("Cadangkan") }
            OutlinedButton(
                onClick = {
                    runCatching { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan500),
                border = BorderStroke(1.dp, Cyan500),
            ) { Text("Pulihkan") }
        }

        Spacer(Modifier.height(Dimens.Space4))
        WovenDivider()
        Spacer(Modifier.height(Dimens.Space4))

        OutlinedButton(
            onClick = {
                showConfirm("Reset semua data ke default? Estimasi & riwayat akan hilang.") {
                    onResetDb()
                    showToast("Data direset ke default")
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(Dimens.RadiusControl),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
            border = BorderStroke(1.dp, Red700.copy(alpha = 0.5f)),
        ) { Text("Reset ke Default") }

        Spacer(Modifier.height(Dimens.Space4))
        WovenDivider()
        Spacer(Modifier.height(Dimens.Space4))

        var aboutOpen by remember { mutableStateOf(false) }
        var helpOpen by remember { mutableStateOf(false) }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
            OutlinedButton(
                onClick = { helpOpen = true },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                border = BorderStroke(1.dp, colors.border),
            ) { Text("Panduan Penggunaan") }
            OutlinedButton(
                onClick = { aboutOpen = true },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                border = BorderStroke(1.dp, colors.border),
            ) { Text("Tentang") }
        }
        if (aboutOpen) {
            AboutDialog(onClose = { aboutOpen = false })
        }
        if (helpOpen) {
            OnboardingDialog(onClose = { helpOpen = false })
        }

        Spacer(Modifier.height(Dimens.Space8))
    }
}
