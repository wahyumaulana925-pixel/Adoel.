import { useState } from 'react'
import { LocalNotifications } from '@capacitor/local-notifications'

function App() {
  const [status, setStatus] = useState('Belum dites')

  const requestPermission = async () => {
    const result = await LocalNotifications.requestPermissions()
    setStatus('Permission: ' + result.display)
  }

  const scheduleTest = async () => {
    await LocalNotifications.schedule({
      notifications: [
        {
          id: 1,
          title: 'Test Adoel V5',
          body: 'Notifikasi muncul 10 detik setelah ditekan',
          schedule: { at: new Date(Date.now() + 10000) },
        },
      ],
    })
    setStatus('Notifikasi dijadwalkan, tunggu 10 detik')
  }

  return (
    <div style={{ padding: 20, fontFamily: 'sans-serif' }}>
      <h1>Test Notifikasi Adoel V5</h1>
      <p>{status}</p>
      <button onClick={requestPermission} style={{ display: 'block', marginBottom: 10, padding: 10 }}>
        1. Minta Izin Notifikasi
      </button>
      <button onClick={scheduleTest} style={{ display: 'block', padding: 10 }}>
        2. Jadwalkan Notifikasi Test (10 detik)
      </button>
    </div>
  )
}

export default App
