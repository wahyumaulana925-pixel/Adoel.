package com.jekael.adoel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.DoffState
import com.jekael.adoel.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Fast doff confirmation opened by tapping a RadarCard directly — the machine is
 * already known (from the card that was tapped), so there's no picker step at all.
 * This is the primary, discoverable path for doffing a machine already being tracked;
 * the swipe-to-reveal DOFF button remains as a shortcut for those who already know it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickDoffSheet(
    mcNo: String,
    state: DoffState,
    onClose: () -> Unit,
    onSubmit: (keterangan: String) -> Unit,
) {
    val mesin = state.db[mcNo]
    var keterangan by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(mcNo) {
        delay(150)
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Zinc900,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Zinc700),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Mc $mcNo",
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black, color = Zinc100),
                )
                if (mesin != null) {
                    Text(
                        text = mesin.tipe.name,
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Zinc500),
                    )
                }
            }
            if (mesin != null) {
                Text(
                    text = mesin.corak,
                    style = TextStyle(fontSize = 13.sp, color = Zinc400),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            FieldLabel("Keterangan (opsional)")
            OutlinedTextField(
                value = keterangan,
                onValueChange = { keterangan = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text("Tulis kondisi lapangan sebebasnya...", color = Zinc600) },
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = Zinc100, fontSize = 15.sp),
                singleLine = false,
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onSubmit(keterangan) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
            ) {
                Text("Selesai Doff", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
