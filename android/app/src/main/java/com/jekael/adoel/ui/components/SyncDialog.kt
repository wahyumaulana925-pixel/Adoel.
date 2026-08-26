package com.jekael.adoel.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.jekael.adoel.data.DoffRepository
import com.jekael.adoel.ui.theme.AppType
import com.jekael.adoel.ui.theme.Cyan600
import com.jekael.adoel.ui.theme.Dimens
import com.jekael.adoel.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SyncDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val repository = remember(context) { DoffRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents ?: return@rememberLauncherForActivityResult
        scope.launch {
            val imported = runCatching {
                withContext(Dispatchers.IO) { repository.processScannedQr(contents, context) }
            }.getOrNull()
            Toast.makeText(
                context,
                if (imported != null) "Sinkronisasi berhasil" else "QR Sync tidak valid",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    FloatingEditDialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space12),
        ) {
            Text("QR Sync", style = AppType.DialogTitle.copy(color = colors.textPrimary))
            SlidingToggle(
                labelLeft = "Kirim",
                labelRight = "Scan",
                selectedIndex = tab,
                onSelect = { tab = it; qrBitmap = null },
                containerColor = colors.bgElevated2,
                activeColorLeft = Cyan600,
                activeColorRight = Cyan600,
                activeTextColorLeft = Color.White,
                activeTextColorRight = Color.White,
                inactiveTextColor = colors.textMuted,
                modifier = Modifier.fillMaxWidth(),
                accessibilityLabel = "Mode QR Sync",
            )
            if (tab == 0) {
                Text("Pilih data yang ingin dikirim", style = AppType.BodySmall.copy(color = colors.textSecondary))
                SyncButton("Bagikan Operan") {
                    scope.launch {
                        qrBitmap = runCatching {
                            withContext(Dispatchers.IO) { createQrBitmap(repository.prepareHandoverData()) }
                        }.getOrNull()
                    }
                }
                SyncButton("Bagikan Daftar Mesin") {
                    scope.launch {
                        qrBitmap = runCatching {
                            withContext(Dispatchers.IO) { createQrBitmap(repository.prepareMasterDbData()) }
                        }.getOrNull()
                    }
                }
                qrBitmap?.let { bitmap ->
                    Spacer(Modifier.height(Dimens.Space4))
                    Box(
                        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(Dimens.RadiusControl)).padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(bitmap.asImageBitmap(), contentDescription = "QR Sync", modifier = Modifier.size(240.dp))
                    }
                }
            } else {
                Text("Arahkan kamera ke QR Sync dari perangkat lain", style = AppType.BodySmall.copy(color = colors.textSecondary))
                SyncButton("Mulai Scan") {
                    scanLauncher.launch(
                        ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt("Scan QR Sync")
                            setBeepEnabled(false)
                        },
                    )
                }
            }
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
            ) { Text("Tutup") }
        }
    }
}

@Composable
private fun SyncButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(Dimens.RadiusControl),
        colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
    ) { Text(label) }
}

private fun createQrBitmap(data: String): Bitmap? = runCatching {
    BarcodeEncoder().encodeBitmap(data, BarcodeFormat.QR_CODE, 800, 800)
}.getOrNull()