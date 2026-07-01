package com.jekael.adoel.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ToastState(
    val key: Int,
    val msg: String,
    val undoAction: (() -> Unit)? = null,
)

data class ConfirmState(
    val msg: String,
    val onConfirm: () -> Unit,
    val onCancel: (() -> Unit)? = null,
)

class UIViewModel : ViewModel() {
    private val _toast = MutableStateFlow<ToastState?>(null)
    val toast: StateFlow<ToastState?> = _toast.asStateFlow()

    private val _confirm = MutableStateFlow<ConfirmState?>(null)
    val confirm: StateFlow<ConfirmState?> = _confirm.asStateFlow()

    private var toastKey = 0

    fun showToast(msg: String, undo: (() -> Unit)? = null) {
        toastKey++
        _toast.value = ToastState(toastKey, msg, undo)
    }

    fun dismissToast() {
        _toast.value = null
    }

    fun showConfirm(msg: String, onConfirm: () -> Unit, onCancel: (() -> Unit)? = null) {
        _confirm.value = ConfirmState(msg, onConfirm, onCancel)
    }

    fun dismissConfirm() {
        _confirm.value = null
    }
}
