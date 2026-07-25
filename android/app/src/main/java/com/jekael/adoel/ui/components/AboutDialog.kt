package com.jekael.adoel.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.BuildConfig
import com.jekael.adoel.ui.theme.*

@Composable
internal fun AboutDialog(onClose: () -> Unit) {
    val colors = LocalAppColors.current
    FloatingEditDialog(onDismissRequest = onClose) {
        Text("Tentang", style = AppType.DialogTitle.copy(color = colors.textPrimary))
        Spacer(Modifier.height(Dimens.Space16))
        Text("Adoel.", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black, color = colors.textPrimary))
        Spacer(Modifier.height(Dimens.Space4))
        Text(
            "Versi ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
            style = AppType.BodySmall.copy(color = colors.textSecondary),
        )
        Spacer(Modifier.height(Dimens.Space20))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.RadiusControl),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
        ) { Text("Tutup", fontWeight = FontWeight.SemiBold) }
    }
}
