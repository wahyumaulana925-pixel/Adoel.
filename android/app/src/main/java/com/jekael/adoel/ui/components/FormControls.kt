package com.jekael.adoel.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.ui.theme.LocalAppColors
import com.jekael.adoel.ui.theme.Teal500

@Composable
fun FieldLabel(text: String) {
    val colors = LocalAppColors.current
    Text(
        text = text.uppercase(),
        style = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = colors.textMuted,
        ),
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
fun outlinedFieldColors(): TextFieldColors {
    val colors = LocalAppColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Teal500,
        unfocusedBorderColor = colors.border,
        cursorColor = Teal500,
        focusedContainerColor = colors.bgElevated2,
        unfocusedContainerColor = colors.bgElevated2,
    )
}
