package com.jekael.adoel.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.ui.theme.Teal500
import com.jekael.adoel.ui.theme.Zinc500
import com.jekael.adoel.ui.theme.Zinc700
import com.jekael.adoel.ui.theme.Zinc800

@Composable
fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = Zinc500,
        ),
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Teal500,
    unfocusedBorderColor = Zinc700,
    cursorColor = Teal500,
    focusedContainerColor = Zinc800,
    unfocusedContainerColor = Zinc800,
)
