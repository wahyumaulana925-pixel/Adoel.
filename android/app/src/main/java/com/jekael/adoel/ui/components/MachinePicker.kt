package com.jekael.adoel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.MesinData
import com.jekael.adoel.ui.theme.*

/**
 * Searchable machine number picker. Shows a text field the operator can type a number
 * or corak into; matching machines drop down below. Once a machine is chosen, the field
 * collapses into a summary chip with a "Ganti" button to pick a different one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachinePicker(
    db: Map<String, MesinData>,
    selectedMcNo: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val selectedMesin = selectedMcNo?.let { db[it] }

    if (selectedMcNo != null && selectedMesin != null) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(Zinc800, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Mc $selectedMcNo",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black, color = Zinc100),
                    )
                    Text(
                        text = selectedMesin.tipe.name,
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Zinc500),
                    )
                }
                Text(
                    text = selectedMesin.corak,
                    style = TextStyle(fontSize = 13.sp, color = Zinc400),
                    maxLines = 1,
                )
            }
            TextButton(onClick = { onSelect(null); query = "" }) {
                Text("Ganti", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Cyan400))
            }
        }
        return
    }

    val filtered = remember(query, db) {
        db.entries
            .filter { (_, v) -> v.corak.isNotEmpty() && v.corak != "-" }
            .filter { (k, v) -> query.isBlank() || k.contains(query, ignoreCase = true) || v.corak.contains(query, ignoreCase = true) }
            .sortedBy { it.key.toIntOrNull() ?: 0 }
            .take(8)
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filtered.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; expanded = true },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            placeholder = { Text("Cari nomor mesin / corak...", color = Zinc600) },
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(color = Zinc100, fontSize = 15.sp),
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded && filtered.isNotEmpty(),
            onDismissRequest = { expanded = false },
            containerColor = Zinc800,
        ) {
            filtered.forEach { (k, v) ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Mc $k", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Zinc100))
                            Text(v.tipe.name, style = TextStyle(fontSize = 11.sp, color = Zinc500))
                            Text(v.corak, style = TextStyle(fontSize = 13.sp, color = Zinc300), maxLines = 1)
                        }
                    },
                    onClick = {
                        onSelect(k)
                        query = k
                        expanded = false
                    },
                )
            }
        }
    }
}
