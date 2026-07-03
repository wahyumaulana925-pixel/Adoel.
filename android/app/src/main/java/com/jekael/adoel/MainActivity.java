package com.jekael.adoel;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannels();
    }
    
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "doffing-alerts",
                "Doffing Notifications",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifikasi estimasi selesai doffing");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 250, 250});
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
