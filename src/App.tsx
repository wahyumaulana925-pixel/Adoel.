import { useEffect, useRef, useState } from 'react'
import { LocalNotifications } from '@capacitor/local-notifications'
import { useDoffStore, jamSekarangAbs, shiftAbsKeJamStr } from './store/useDoffStore'
import DaftarMesin from './components/DaftarMesin'
import Aktual from './components/Aktual'

type Mode = 'aktual' | 'estimasi'

function App() {
  const { db, estimasi, prosesBarisKondisiMesin, prosesBarisUmum, hapusEstimasi } = useDoffStore()
  const [cmd, setCmd] = useState('')
  const [mode, setMode] = useState<Mode>('aktual')
  const [toast, setToast] = useState<{ msg: string; ok: boolean } | null>(null)
  const [tab, setTab] = useState<'utama' | 'riwayat' | 'mesin'>('utama')
  const [permGranted, setPermGranted] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    LocalNotifications.checkPermissions().then((r) => setPermGranted(r.display === 'granted'))
  }, [])

  useEffect(() => {
    if (!toast) return
    const t = setTimeout(() => setToast(null), 1800)
    return () => clearTimeout(t)
  }, [toast])

  const mintaIzin = async () => {
    const r = await LocalNotifications.requestPermissions()
    setPermGranted(r.display === 'granted')
  }

  const scheduleNotif = async (mcNo: string, estAbsMin: number) => {
    const fireTime = estAbsMin * 60000 - 5 * 60000
    if (fireTime - Date.now() <= 0) return
    await LocalNotifications.schedule({
      notifications: [
        {
          id: parseInt(mcNo, 10),
          title: `Mc ${mcNo} siap doffing`,
          body: `Estimasi waktu doffing Mc ${mcNo} tercapai (${shiftAbsKeJamStr(estAbsMin)})`,
          schedule: { at: new Date(fireTime), allowWhileIdle: true },
        },
      ],
    })
  }

  const kirimCmd = async () => {
    const ln = cmd.trim()
    if (!ln) return
    const mcMatch = ln.match(/^(\d{1,3})\s*(.*)$/)
    if (!mcMatch) {
      setToast({ msg: 'Format tidak valid (Mc: angka)', ok: false })
      return
    }
    const mcNo = mcMatch[1]
    const rest = mcMatch[2]

    if (mode === 'estimasi') {
      if (!rest) {
        setToast({ msg: 'Masukkan nilai estimasi', ok: false })
        return
      }
      const res = prosesBarisKondisiMesin(ln, jamSekarangAbs())
      if (res.type === 'ok' && res.mcNo && res.estAbs) {
        await scheduleNotif(res.mcNo, Math.round(res.estAbs))
        setToast({ msg: `Est Mc ${mcNo} disimpan -> ${shiftAbsKeJamStr(res.estAbs)}`, ok: true })
        setCmd('')
      } else {
        const looksLikeEstVal = /y$|m$|^c$/i.test(rest.split(/\s+/)[0] || '')
        if (looksLikeEstVal) {
          setToast({ msg: res.msg, ok: false })
        } else {
          const resAkt = prosesBarisUmum(ln)
          if (resAkt.type === 'ok' && resAkt.mcNo) {
            await LocalNotifications.cancel({ notifications: [{ id: parseInt(resAkt.mcNo, 10) }] })
            setToast({ msg: `Mc ${mcNo} Doffed!`, ok: true })
            setCmd('')
          } else {
            setToast({ msg: resAkt.msg || 'Gagal', ok: false })
          }
        }
      }
    } else {
      const res = prosesBarisUmum(ln)
      if (res.type === 'ok' && res.mcNo) {
        await LocalNotifications.cancel({ notifications: [{ id: parseInt(res.mcNo, 10) }] })
        setToast({ msg: `Mc ${mcNo} Doffed!`, ok: true })
        setCmd('')
      } else {
        setToast({ msg: res.msg || 'Gagal', ok: false })
      }
    }
    inputRef.current?.focus()
  }

  const batalEstimasi = async (mcNo: string) => {
    hapusEstimasi(mcNo)
    await LocalNotifications.cancel({ notifications: [{ id: parseInt(mcNo, 10) }] })
  }

  const mulaiDoffing = (mcNo: string) => {
    setMode('aktual')
    setCmd(mcNo + ' ')
    setTab('utama')
    setTimeout(() => inputRef.current?.focus(), 50)
  }

  const daftarEstimasi = Object.values(estimasi).sort((a, b) => a.estAbsMin - b.estAbsMin)

  return (
    <div style={{ fontFamily: 'sans-serif' }}>
      <div style={{ display: 'flex', borderBottom: '1px solid #eee', maxWidth: 480, margin: '0 auto' }}>
        {(['utama', 'riwayat', 'mesin'] as const).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            style={{
              flex: 1,
              padding: 12,
              background: tab === t ? '#0066ff' : '#fff',
              color: tab === t ? '#fff' : '#333',
              fontWeight: 600,
              textTransform: 'capitalize',
            }}
          >
            {t}
          </button>
        ))}
      </div>

      {tab === 'mesin' && <DaftarMesin />}
      {tab === 'riwayat' && <Aktual />}

      {tab === 'utama' && (
        <div style={{ padding: 16, maxWidth: 480, margin: '0 auto' }}>
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

          {/* Mode toggle pill */}
          <div
            style={{
              position: 'relative',
              display: 'flex',
              background: '#eee',
              borderRadius: 10,
              padding: 4,
              marginBottom: 8,
            }}
          >
            <button
              onClick={() => setMode('aktual')}
              style={{
                flex: 1,
                padding: 10,
                borderRadius: 8,
                fontWeight: 700,
                background: mode === 'aktual' ? '#06b6d4' : 'transparent',
                color: mode === 'aktual' ? '#fff' : '#666',
                transition: 'all .2s',
              }}
            >
              Aktual
            </button>
            <button
              onClick={() => setMode('estimasi')}
              style={{
                flex: 1,
                padding: 10,
                borderRadius: 8,
                fontWeight: 700,
                background: mode === 'estimasi' ? '#f59e0b' : 'transparent',
                color: mode === 'estimasi' ? '#fff' : '#666',
                transition: 'all .2s',
              }}
            >
              Estimasi
            </button>
          </div>

          <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
            <input
              ref={inputRef}
              value={cmd}
              onChange={(e) => setCmd(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && kirimCmd()}
              placeholder={mode === 'aktual' ? 'Ketik aktual (31 hb)...' : 'Ketik estimasi (61 150y)...'}
              style={{
                flex: 1,
                padding: 12,
                border: '1px solid #ccc',
                borderRadius: 8,
                fontFamily: 'monospace',
              }}
            />
            <button
              onClick={kirimCmd}
              style={{
                padding: '0 18px',
                background: mode === 'aktual' ? '#06b6d4' : '#f59e0b',
                color: '#fff',
                borderRadius: 8,
                fontWeight: 700,
              }}
            >
              Kirim
            </button>
          </div>

          {toast && (
            <div
              style={{
                padding: 10,
                marginBottom: 12,
                borderRadius: 8,
                fontSize: 13,
                background: toast.ok ? '#dcfce7' : '#fee2e2',
                color: toast.ok ? '#166534' : '#991b1b',
              }}
            >
              {toast.msg}
            </div>
          )}

          <h2 style={{ fontSize: 16, fontWeight: 600, marginTop: 8, marginBottom: 8 }}>
            Estimasi Aktif ({daftarEstimasi.length})
          </h2>
          {daftarEstimasi.length === 0 && (
            <p style={{ fontSize: 13, color: '#999' }}>Belum ada estimasi.</p>
          )}
          {daftarEstimasi.map((e) => {
            const mesin = db[e.mcNo]
            const sisaMin = e.estAbsMin - jamSekarangAbs()
            const urgent = sisaMin <= 10
            return (
              <div
                key={e.mcNo}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '10px 12px',
                  border: urgent ? '1px solid #fca5a5' : '1px solid #eee',
                  background: urgent ? '#fef2f2' : '#fff',
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
                  <div style={{ fontWeight: 600, color: urgent ? '#dc2626' : '#111' }}>
                    {shiftAbsKeJamStr(e.estAbsMin)}
                  </div>
                  <div style={{ display: 'flex', gap: 6, marginTop: 4 }}>
                    <button
                      onClick={() => mulaiDoffing(e.mcNo)}
                      style={{ fontSize: 11, color: '#06b6d4', fontWeight: 700 }}
                    >
                      Doffing
                    </button>
                    <button
                      onClick={() => batalEstimasi(e.mcNo)}
                      style={{ fontSize: 11, color: '#cc0000' }}
                    >
                      Batal
                    </button>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default App
