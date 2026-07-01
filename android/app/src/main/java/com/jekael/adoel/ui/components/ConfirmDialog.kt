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
import com.jekael.adoel.ui.theme.*
import com.jekael.adoel.viewmodel.ConfirmState

@Composable
fun ConfirmDialog(
    confirm: ConfirmState?,
    onDismiss: () -> Unit,
) {
    if (confirm == null) return
    AlertDialog(
        onDismissRequest = { confirm.onCancel?.invoke(); onDismiss() },
        containerColor = Zinc900,
        titleContentColor = Zinc100,
        textContentColor = Zinc400,
        title = {
            Text(
                text = "Konfirmasi",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Zinc100),
            )
        },
        text = {
            Text(
                text = confirm.msg,
                style = TextStyle(fontSize = 14.sp, color = Zinc400),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { confirm.onConfirm(); onDismiss() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Red400),
            ) {
                Text("Ya", style = TextStyle(fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { confirm.onCancel?.invoke(); onDismiss() },
                colors = ButtonDefaults.textButtonColors(contentColor = Zinc400),
            ) {
                Text("Batal")
            }
        },
    )
}
