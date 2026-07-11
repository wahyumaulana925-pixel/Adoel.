package com.jekael.adoel.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Send-button feedback on a successful console submit: brief scale bounce + checkmark flash.
 * Held via `remember { SendPulseState() }` in MainScreen — grouping the trio here (instead of 3
 * separate top-level `remember`s) is pure organization, the Compose-tracked fields behave the same. */
internal class SendPulseState {
    var key by mutableIntStateOf(0)
    val scale = Animatable(1f)
    var showCheck by mutableStateOf(false)
}

/** Brief red-ring flash + haptic on a rejected console command — pairs with MainScreen's
 * flashError() so a failure isn't easy to miss if the operator looks away right after submitting. */
internal class ErrorFlashState {
    var key by mutableIntStateOf(0)
    var active by mutableStateOf(false)
}

/** "Selesai Shift" celebration: checkmark scale-in over a dimming backdrop, both fading out on
 * their own after a beat. */
internal class ShiftFinishedState {
    var key by mutableIntStateOf(0)
    var visible by mutableStateOf(false)
    val checkScale = Animatable(0f)
    val backdropAlpha = Animatable(0f)
}

/** System permission/setting flags, refreshed on ON_RESUME (see MainScreen's DisposableEffect) —
 * notification permission, exact-alarm scheduling, and battery-optimization exemption. */
internal class PermissionState {
    var notifGranted by mutableStateOf(true)
    var exactAlarmGranted by mutableStateOf(true)
    var batteryUnrestricted by mutableStateOf(true)
}
