# Konfigurasi Notifikasi Adoel

## Ikon Notifikasi

### Small Icon
- File: `android/app/src/main/res/drawable/ic_stat_notify.xml`
- Format: Vector drawable dengan warna putih (#FFFFFF)
- Ukuran: 192dp x 192dp
- **Persyaratan Android**: Icon harus monokromatik (putih)

### Large Icon  
- Menggunakan `ic_launcher` (ikon aplikasi utama)
- Ditampilkan di notifikasi yang diperluas

## Warna Notifikasi

- Primary Color: `#0066FF` (biru)
- Dikonfigurasi di: `android/app/src/main/res/values/colors.xml`

## Notification Channels (Android 8+)

Untuk hasil optimal, buat notification channel di MainActivity:

```kotlin
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "doffing-alerts",  // channel ID
            "Doffing Notifications",  // user-visible name
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikasi estimasi selesai doffing"
        }
        
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}
```

**Kemudian panggil di onCreate():**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    createNotificationChannels()
}
```

## Sound & Vibration

Untuk menambahkan sound dan vibration pada notifikasi, update di `scheduleNotif`:

```typescript
smallIcon: 'ic_stat_notify',
largeIcon: 'ic_launcher',
sound: true,           // Enable sound
vibrate: [0, 250, 250, 250],  // Vibration pattern
```

## Testing Notifikasi

Untuk test notifikasi di development:

```bash
# Build Android
npm run build
npx cap build android

# Jalankan di emulator/device
npx cap run android
```

Kemudian masukkan data estimasi di app untuk trigger notifikasi 5 menit sebelum waktu estimasi.

## Troubleshooting

### Notifikasi tidak muncul
1. Cek permission di AndroidManifest.xml (POST_NOTIFICATIONS)
2. Verifikasi smallIcon sudah ada di drawable folder
3. Periksa notification channel configuration

### Icon terlihat buram/salah
- Pastikan icon format SVG/XML vector drawable
- Icon harus monokromatik (hanya putih & transparan)
- Jangan gunakan warna di dalam icon drawable

### Sound/Vibration tidak bekerja
- Cek device settings untuk notification permissions
- Verify notification channel configuration
- Test di device fisik (emulator kadang terbatas)
