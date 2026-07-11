package com.jekael.adoel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jekael.adoel.ui.theme.Cyan500
import com.jekael.adoel.ui.theme.LocalAppColors

/** Cold-start placeholder shown until [com.jekael.adoel.viewmodel.DoffViewModel]'s first persisted
 * state emission arrives, so the 174-unconfigured-machine in-memory default never flashes on screen. */
@Composable
internal fun LoadingPlaceholder() {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Cyan500)
    }
}
