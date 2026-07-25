package com.jekael.adoel.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jekael.adoel.data.MesinData
import com.jekael.adoel.data.MesinTipe
import com.jekael.adoel.data.absMinToTimeStr
import com.jekael.adoel.data.estAbsD408
import com.jekael.adoel.data.estimasiFieldHint
import com.jekael.adoel.data.nowAbsMin
import com.jekael.adoel.data.parseDurasi
import com.jekael.adoel.data.parseJam
import com.jekael.adoel.data.sisaMenitD405
import com.jekael.adoel.ui.theme.AppType
import com.jekael.adoel.ui.theme.Cyan500
import com.jekael.adoel.ui.theme.Cyan600
import com.jekael.adoel.ui.theme.Dimens
import com.jekael.adoel.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Terpandu (guided) ESTIMASI entry — one field whose label/keyboard adapt to the tapped
 * machine's [MesinTipe], with a live "≈ jam" preview computed from the exact same pure formulas
 * DoffViewModel uses, so what's previewed here is guaranteed to match what actually gets saved.
 * On Simpan, builds the identical "$mcNo $value" string the console used to send in Teks mode and
 * hands it to [onSubmit] — the caller routes it through handlers.handleCommand so the overwrite
 * guard, notification scheduling, and toast/haptic feedback all still apply.
 *
 * A machine with no corak set yet (fresh install, or one that was never configured in Pengaturan)
 * gets an inline quick-setup step first instead of being turned away to go set it up elsewhere —
 * corak/target yard are the two fields that change constantly on the floor (Master Blueprint §4C),
 * so this dialog is the fast path for them same as [QuickEditCorakDialog] is from a RadarCard tap.
 */
@Composable
fun GuidedEstimasiSheet(
    mcNo: String,
    mesin: MesinData?,
    onDismiss: () -> Unit,
    onSubmit: (value: String) -> Unit,
    onQuickUpdate: (corak: String, targetYard: Double?) -> Unit,
) {
    val colors = LocalAppColors.current
    val tipe = mesin?.tipe ?: MesinTipe.TAPPET
    var needQuickCorakSetup by remember(mcNo) { mutableStateOf(mesin == null || mesin.corak.isBlank() || mesin.corak.trim() == "-") }

    var corakInput by remember(mcNo) { mutableStateOf("") }
    var targetYardInput by remember(mcNo) { mutableStateOf("") }
    var valueInput by remember(mcNo) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(mcNo, needQuickCorakSetup) {
        delay(100)
        focusRequester.requestFocus()
    }

    FloatingEditDialog(onDismissRequest = onDismiss) {
        Text(
            text = "Update Estimasi — Mc $mcNo (${tipe.name})",
            style = AppType.DialogTitle.copy(color = colors.textPrimary),
        )
        Spacer(Modifier.height(Dimens.Space16))

        if (needQuickCorakSetup) {
            FieldLabel("Masukkan Corak Baru")
            OutlinedTextField(
                value = corakInput,
                onValueChange = { corakInput = it.uppercase() },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                placeholder = { Text("cth: 34758", color = colors.textFaint) },
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                textStyle = AppType.FieldText.copy(color = colors.textPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            Spacer(Modifier.height(Dimens.Space12))
            FieldLabel("Target Yard (Opsional)")
            OutlinedTextField(
                value = targetYardInput,
                onValueChange = { targetYardInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("cth: 303", color = colors.textFaint) },
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                textStyle = AppType.FieldText.copy(color = colors.textPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            Spacer(Modifier.height(Dimens.Space20))
            Button(
                onClick = {
                    if (corakInput.isNotBlank()) {
                        val yard = targetYardInput.trim().replace(',', '.').toDoubleOrNull()
                        onQuickUpdate(corakInput.trim(), yard)
                        needQuickCorakSetup = false
                    }
                },
                enabled = corakInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
            ) { Text("Simpan Corak & Lanjut", fontWeight = FontWeight.SemiBold) }
        } else {
            val hint = estimasiFieldHint(tipe)
            val preview = remember(valueInput, mesin) { previewEstimasi(tipe, valueInput, mesin) }

            // One-shot cyan "water jet" pulse sweeping from the Simpan button toward the field's
            // left edge on submit — echoes the nozzle spraying the weft thread across the loom
            // (Master Blueprint §3F). onSubmit's success path clears activeOverlay synchronously
            // (see MainScreen's onCleared callback), which tears down this whole dialog the instant
            // it's called — so the animation has to run to completion *before* onSubmit fires, not
            // alongside it as fire-and-forget, or the dialog would vanish before a single animated
            // frame ever reached the screen.
            val nozzleProgress = remember { Animatable(0f) }
            val scope = rememberCoroutineScope()
            fun submit() {
                if (valueInput.isBlank()) return
                val toSubmit = "$mcNo $valueInput"
                scope.launch {
                    nozzleProgress.snapTo(0f)
                    nozzleProgress.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
                    onSubmit(toSubmit)
                }
            }

            FieldLabel(hint.label)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { valueInput = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text("cth: ${hint.example}", color = colors.textFaint) },
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(Dimens.RadiusControl),
                    textStyle = AppType.FieldText.copy(color = colors.textPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    singleLine = true,
                )
                if (nozzleProgress.value > 0f && nozzleProgress.value < 1f) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val progress = nozzleProgress.value
                        val y = size.height / 2f
                        val headX = size.width * (1f - progress)
                        drawLine(
                            color = Cyan500.copy(alpha = (1f - progress) * 0.9f),
                            start = Offset(size.width, y),
                            end = Offset(headX, y),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()), phase = progress * 40f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = preview?.let { "≈ jam $it" } ?: "Isi untuk melihat perkiraan jam",
                style = AppType.Caption.copy(color = if (preview != null) Cyan600 else colors.textFaint),
            )

            Spacer(Modifier.height(Dimens.Space20))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(Dimens.RadiusControl),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                    border = BorderStroke(1.dp, colors.border),
                ) { Text("Batal") }
                Button(
                    onClick = ::submit,
                    enabled = valueInput.isNotBlank(),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(Dimens.RadiusControl),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
                ) { Text("Simpan", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

private fun previewEstimasi(tipe: MesinTipe, raw: String, mesin: MesinData?): String? {
    if (raw.isBlank()) return null
    val now = nowAbsMin()
    return when (tipe) {
        MesinTipe.TAPPET, MesinTipe.CAM -> parseDurasi(raw)?.let { sisa -> absMinToTimeStr(now + sisa) }
        MesinTipe.D405 -> {
            val target = mesin?.targetYard
            val speed = mesin?.speed
            val yardBerjalan = raw.trim().trimEnd('y', 'Y').replace(',', '.').toDoubleOrNull()
            if (yardBerjalan != null && target != null && speed != null && speed > 0) {
                absMinToTimeStr(now + sisaMenitD405(target, yardBerjalan, speed))
            } else null
        }
        MesinTipe.D408 -> {
            val koreksi = mesin?.koreksi
            val jamMin = parseJam(raw)
            if (jamMin != null && koreksi != null) absMinToTimeStr(estAbsD408(jamMin, koreksi, now)) else null
        }
    }
}
