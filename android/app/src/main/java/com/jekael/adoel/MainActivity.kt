package com.jekael.adoel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jekael.adoel.notification.NotificationHelper
import com.jekael.adoel.ui.MainScreen
import com.jekael.adoel.ui.theme.AdoelTheme
import com.jekael.adoel.viewmodel.DoffViewModel
import com.jekael.adoel.viewmodel.UIViewModel

class MainActivity : ComponentActivity() {
    private val doffVm: DoffViewModel by viewModels()
    private val uiVm: UIViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannel(this)

        setContent {
            AdoelTheme {
                MainScreen(doffVm = doffVm, uiVm = uiVm)
            }
        }
    }
}
