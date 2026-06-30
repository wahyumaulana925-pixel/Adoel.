import { useEffect, useState } from 'react'
import { LocalNotifications } from '@capacitor/local-notifications'
import { useDoffStore, jamSekarangAbs, shiftAbsKeJamStr } from './store/useDoffStore'

function App() {
  const { db, estimasi, prosesBarisKondisiMesin, hapusEstimasi } = useDoffStore()
  const [input, setInput] = useState('')
  const [log, setLog] = useState<string[]>([])
  const [permGranted, setPermGranted] = useState(false)

  useEffect(() => {
    LocalNotifications.checkPermissions().then((r) => {
      setPermGranted(r.display === 'granted')
    })
  }, [])

  const mintaIzin = async () => {
    const r = await LocalNotifications.requestPermissions()
    setPermGranted(r.display === 'granted')
  }

  const scheduleNotif = async (mcNo: string, estAbsMin: number) => {
    const fireTime = estAbsMin * 60000 - 5 * 60000
    const delay = fireTime - Date.now()
    if (delay <= 0) return
    const id = parseInt(mcNo, 10)
    await LocalNotifications.schedule({
      notifications: [
        {
          id,
          title: `Mc ${mcNo} siap doffing`,
          body: `Estimasi waktu doffing Mc ${mcNo} tercapai (${shiftAbsKeJamStr(estAbsMin)})`,
          schedule: { at: new Date(fireTime) },
        },
      ],
    })
  }

  const prosesInput = async () => {
    const lines = input.split('\n').map((l) => l.trim()).filter(Boolean)
    const jamKelilingAbs = jamSekarangAbs()
    const hasil: string[] = []
    for (const ln of lines) {
      const res = prosesBarisKondisiMesin(ln, jamKelilingAbs)
      hasil.push(res.msg)
      if (res.type === 'ok' && res.mcNo && res.estAbs) {
        await scheduleNotif(res.mcNo, Math.round(res.estAbs))
      }
    }
    setLog(hasil)
    setInput('')
  }

  const batalEstimasi = async (mcNo: string) => {
    hapusEstimasi(mcNo)
    await LocalNotifications.cancel({ notifications: [{ id: parseInt(mcNo, 10) }] })
  }

  const daftarEstimasi = Object.values(estimasi).sort((a, b) => a.estAbsMin - b.estAbsMin)

  return (
    <div style={{ padding: 16, fontFamily: 'sans-serif', maxWidth: 480, margin: '0 auto' }}>
      <h1 style={{ fontSize: 20, fontWeight: 700, marginBottom: 4 }}>Adoel V5</h1>
      <p style={{ fontSize: 13, color: '#666', marginBottom: 12 }}>
        {db && Object.keys(db).length} mesin terdaftar
      </p>

      {!permGranted && (
        <button
          onClick={mintaIzin}
          style={{ width: '100%', padding: 10, marginBottom: 12, background: '#222', color: '#fff', borderRadius: 8 }}
        >
          Aktifkan Izin Notifikasi
        </button>
      )}

      <textarea
        value={input}
        onChange={(e) => setInput(e.target.value)}
        placeholder={'Contoh:\n12 1.30\n65 250y\n80 c 14.20'}
        rows={5}
        style={{ width: '100%', padding: 10, border: '1px solid #ccc', borderRadius: 8, fontFamily: 'monospace' }}
      />
      <button
        onClick={prosesInput}
        style={{ width: '100%', padding: 10, marginTop: 8, background: '#0066ff', color: '#fff', borderRadius: 8 }}
      >
        Proses Baris
      </button>

      {log.length > 0 && (
        <div style={{ marginTop: 12, fontSize: 13, background: '#f5f5f5', padding: 10, borderRadius: 8 }}>
          {log.map((l, i) => (
            <div key={i}>{l}</div>
          ))}
        </div>
      )}

      <h2 style={{ fontSize: 16, fontWeight: 600, marginTop: 20, marginBottom: 8 }}>
        Estimasi Aktif ({daftarEstimasi.length})
      </h2>
      {daftarEstimasi.length === 0 && (
        <p style={{ fontSize: 13, color: '#999' }}>Belum ada estimasi.</p>
      )}
      {daftarEstimasi.map((e) => {
        const mesin = db[e.mcNo]
        return (
          <div
            key={e.mcNo}
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              padding: '10px 12px',
              border: '1px solid #eee',
              borderRadius: 8,
              marginBottom: 6,
            }}
          >
            <div>
              <div style={{ fontWeight: 600 }}>Mc {e.mcNo}</div>
              <div style={{ fontSize: 12, color: '#666' }}>
                {mesin?.tipe} · corak {mesin?.corak}
              </div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontWeight: 600 }}>{shiftAbsKeJamStr(e.estAbsMin)}</div>
              <button
                onClick={() => batalEstimasi(e.mcNo)}
                style={{ fontSize: 11, color: '#cc0000', marginTop: 4 }}
              >
                Batalkan
              </button>
            </div>
          </div>
        )
      })}
    </div>
  )
}

export default App
