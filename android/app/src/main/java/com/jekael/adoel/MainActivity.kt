package com.jekael.adoel

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jekael.adoel.notification.NotificationHelper
import com.jekael.adoel.ui.MainScreen
import com.jekael.adoel.ui.theme.AdoelTheme
import com.jekael.adoel.viewmodel.DoffViewModel
import com.jekael.adoel.viewmodel.UIViewModel

class MainActivity : ComponentActivity() {
    private val doffVm: DoffViewModel by viewModels()
    private val uiVm: UIViewModel by viewModels()

    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* jika ditolak, alarm tetap terjadwal tapi notifikasi tidak akan tampil */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            AdoelTheme {
                MainScreen(doffVm = doffVm, uiVm = uiVm)
            }
        }
    }
}
