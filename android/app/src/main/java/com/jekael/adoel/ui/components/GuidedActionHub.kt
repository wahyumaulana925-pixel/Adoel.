package com.jekael.adoel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jekael.adoel.ui.theme.AppType
import com.jekael.adoel.ui.theme.Cyan600
import com.jekael.adoel.ui.theme.Dimens
import com.jekael.adoel.ui.theme.Emerald500
import com.jekael.adoel.ui.theme.LocalAppColors

/** Smart entry-point dialog for a machine typed into the console that has no active estimasi yet
 * — offers the two things an operator could mean instead of guessing: start timing it, or record
 * a doff it forgot to time in the first place. Machines that already have an active estimasi skip
 * this entirely and jump straight to [GuidedEstimasiSheet] (see MainScreen's onGuidedStart). */
@Composable
fun GuidedActionHub(
    mcNo: String,
    onDismiss: () -> Unit,
    onAddEstimasiClick: () -> Unit,
    onDirectDoffClick: () -> Unit,
) {
    val colors = LocalAppColors.current

    FloatingEditDialog(onDismissRequest = onDismiss) {
        Text("Mc $mcNo belum di-timer", style = AppType.DialogTitle.copy(color = colors.textPrimary))
        Spacer(Modifier.height(4.dp))
        Text(
            "Pilih tindakan yang ingin dilakukan pada mesin ini.",
            style = AppType.Caption.copy(color = colors.textMuted),
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HubActionButton(
                label = "Tambah Estimasi",
                icon = Icons.Outlined.Schedule,
                accent = Cyan600,
                onClick = onAddEstimasiClick,
                modifier = Modifier.weight(1f),
            )
            HubActionButton(
                label = "Catat Potong (Doff)",
                icon = Icons.Outlined.Check,
                accent = Emerald500,
                onClick = onDirectDoffClick,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(Dimens.RadiusControl),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
            border = BorderStroke(1.dp, colors.border),
        ) { Text("Batal") }
    }
}

@Composable
private fun HubActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 68.dp),
        shape = RoundedCornerShape(Dimens.RadiusControl),
        colors = ButtonDefaults.buttonColors(containerColor = accent),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(label, style = AppType.TabLabel.copy(color = Color.White), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
