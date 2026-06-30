import { useState, useEffect, useRef, useMemo } from 'react'
import { StatusBar, Style } from '@capacitor/status-bar'
import { useDoffStore, jamSekarangAbs, jamSekarangLabel } from './store/useDoffStore'
import { useUIStore } from './store/useUIStore'
import { scheduleNotif, cancelNotif, checkPermission, ensurePermission } from './lib/notifications'
import { RadarCard } from './components/RadarCard'
import { HistoryDrawer } from './components/HistoryDrawer'
import { SettingsDrawer } from './components/SettingsDrawer'
import { EditEstSheet } from './components/EditEstSheet'
import { EditAktSheet } from './components/EditAktSheet'
import { Toast } from './components/Toast'
import { ConfirmModal } from './components/ConfirmModal'

type Mode = 'estimasi' | 'aktual'

function GearIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
    </svg>
  )
}

function HistoryIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
      <line x1="3" y1="6" x2="21" y2="6" />
      <line x1="3" y1="12" x2="21" y2="12" />
      <line x1="3" y1="18" x2="21" y2="18" />
    </svg>
  )
}

function BellOffIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-4 h-4 shrink-0">
      <path d="M13.73 21a2 2 0 0 1-3.46 0" />
      <path d="M18.63 13A17.9 17.9 0 0 1 18 8" />
      <path d="M6.26 6.26A5.86 5.86 0 0 0 6 8c0 7-3 9-3 9h14" />
      <path d="M18 8a6 6 0 0 0-9.33-5" />
      <line x1="1" y1="1" x2="23" y2="23" />
    </svg>
  )
}

function SendIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
      <line x1="22" y1="2" x2="11" y2="13" />
      <polygon points="22 2 15 22 11 13 2 9 22 2" />
    </svg>
  )
}

export default function App() {
  const store = useDoffStore()
  const { showToast } = useUIStore()

  const [mode, setMode] = useState<Mode>('aktual')
  const [input, setInput] = useState('')
  const [clock, setClock] = useState(jamSekarangLabel())
  const [nowAbs, setNowAbs] = useState(jamSekarangAbs())
  const [notifGranted, setNotifGranted] = useState(true)

  const [historyOpen, setHistoryOpen] = useState(false)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [editEstMc, setEditEstMc] = useState<string | null>(null)
  const [editAktId, setEditAktId] = useState<number | null>(null)

  const inputRef = useRef<HTMLInputElement>(null)

  // White status bar icons on dark header
  useEffect(() => {
    StatusBar.setStyle({ style: Style.Dark }).catch(() => {})
  }, [])

  // Notification permission check
  useEffect(() => {
    checkPermission().then(setNotifGranted)
  }, [])

  // Clock tick every 5 s
  useEffect(() => {
    const id = setInterval(() => {
      setClock(jamSekarangLabel())
      setNowAbs(jamSekarangAbs())
    }, 5000)
    return () => clearInterval(id)
  }, [])

  const handleRequestNotifPermission = async () => {
    const granted = await ensurePermission()
    setNotifGranted(granted)
    if (!granted) showToast('⚠ Izin notifikasi ditolak')
  }

  const allTouched = useMemo(() => {
    const s = new Set<string>()
    Object.keys(store.estimasi).forEach((k) => s.add(k))
    store.aktual.forEach((a) => s.add(a.mcNo))
    return s
  }, [store.estimasi, store.aktual])

  const radarList = useMemo(
    () => Object.values(store.estimasi).sort((a, b) => a.estAbsMin - b.estAbsMin),
    [store.estimasi]
  )

  const handleCommand = async () => {
    const cmd = input.trim().toUpperCase()
    if (!cmd) return

    if (mode === 'estimasi') {
      const result = store.prosesBarisKondisiMesin(cmd, jamSekarangAbs())
      if (result.type === 'ok') {
        showToast(result.msg)
        setInput('')
        if (result.mcNo && result.estAbs) await scheduleNotif(result.mcNo, result.estAbs)
      } else {
        showToast(`⚠ ${result.msg}`)
      }
    } else {
      const result = store.prosesBarisUmum(cmd)
      if (result.type === 'ok') {
        const { mcNo, prevEst } = result
        if (mcNo) await cancelNotif(mcNo)
        showToast(result.msg, async () => {
          result.undoFn!()
          if (prevEst) await scheduleNotif(prevEst.mcNo, prevEst.estAbsMin)
        })
        setInput('')
      } else {
        showToast(`⚠ ${result.msg}`)
      }
    }
    inputRef.current?.focus()
  }

  const handleDoff = async (mcNo: string) => {
    const result = store.prosesBarisUmum(mcNo)
    if (result.type === 'ok') {
      const { prevEst } = result
      await cancelNotif(mcNo)
      showToast(result.msg, async () => {
        result.undoFn!()
        if (prevEst) await scheduleNotif(prevEst.mcNo, prevEst.estAbsMin)
      })
    } else {
      showToast(`⚠ ${result.msg}`)
    }
  }

  const handleHapusEst = async (mcNo: string) => {
    const prevEst = store.estimasi[mcNo]
    store.hapusEstimasi(mcNo)
    await cancelNotif(mcNo)
    showToast(`Mc ${mcNo} dihapus`, async () => {
      if (prevEst) {
        store.restoreEstimasi(prevEst)
        await scheduleNotif(prevEst.mcNo, prevEst.estAbsMin)
      }
    })
  }

  const doffCount = store.aktual.length
  const totalMc   = allTouched.size

  return (
    <div className="h-screen flex flex-col bg-zinc-950 text-zinc-100 overflow-hidden">

      {/* ── Header ── */}
      <header className="flex-shrink-0 bg-zinc-950 border-b border-zinc-800/60 pt-safe">
        <div className="flex items-center gap-3 px-4 h-12">
          {/* Branding */}
          <span className="font-black text-[15px] text-zinc-100 tracking-tight leading-none">
            Adoel<span className="text-teal-400">.</span>
          </span>

          {/* Clock — centered */}
          <span className="flex-1 text-center text-sm font-semibold text-zinc-400 tabular-nums">
            {clock}
          </span>

          {/* Right cluster */}
          <div className="flex items-center gap-1.5">
            {/* Doff counter */}
            <div className="flex items-baseline gap-0.5 px-2 py-1 rounded-lg bg-zinc-900">
              <span className="text-sm font-bold text-zinc-200 tabular-nums">{doffCount}</span>
              <span className="text-xs text-zinc-700">/</span>
              <span className="text-xs text-zinc-500 tabular-nums">{totalMc}</span>
            </div>
            {/* Settings */}
            <button
              className="w-9 h-9 flex items-center justify-center text-zinc-500 active:text-zinc-200 rounded-xl active:bg-zinc-800 transition-colors duration-150"
              onClick={() => setSettingsOpen(true)}
            >
              <GearIcon />
            </button>
          </div>
        </div>
      </header>

      {/* ── Notification permission banner ── */}
      {!notifGranted && (
        <button
          className="flex-shrink-0 flex items-center justify-center gap-2 bg-amber-950/60 border-b border-amber-900/50 text-amber-400 text-xs py-2 px-4 w-full transition-colors duration-150 active:bg-amber-950"
          onClick={handleRequestNotifPermission}
        >
          <BellOffIcon />
          <span>Notifikasi nonaktif — ketuk untuk izinkan</span>
        </button>
      )}

      {/* ── Main radar card list ── */}
      <main className="flex-1 overflow-y-auto hide-scroll py-2.5 px-3">
        {radarList.length === 0 ? (
          <EmptyState mode={mode} />
        ) : (
          <div className="flex flex-col gap-2">
            {radarList.map((est) => (
              <RadarCard
                key={est.mcNo}
                est={est}
                mesin={store.db[est.mcNo]}
                nowAbs={nowAbs}
                onDoff={() => handleDoff(est.mcNo)}
                onHapus={() => handleHapusEst(est.mcNo)}
                onLongPress={() => setEditEstMc(est.mcNo)}
              />
            ))}
          </div>
        )}
      </main>

      {/* ── Footer: mode toggle + command input ── */}
      <footer className="flex-shrink-0 px-3 pt-2 pb-safe-floor border-t border-zinc-800/60 bg-zinc-950">
        {/* Mode toggle */}
        <div className="flex mb-2 bg-zinc-900 rounded-xl p-0.5 gap-0.5">
          {([['estimasi', 'Estimasi'], ['aktual', 'Doff']] as const).map(([m, label]) => (
            <button
              key={m}
              className={`flex-1 py-1.5 text-xs rounded-[10px] font-semibold transition-colors duration-200 ${
                mode === m
                  ? 'bg-teal-500 text-white'
                  : 'text-zinc-500 active:text-zinc-300'
              }`}
              onClick={() => setMode(m)}
            >
              {label}
            </button>
          ))}
        </div>

        {/* Command row */}
        <div className="flex gap-2">
          <input
            ref={inputRef}
            type="text"
            inputMode="text"
            value={input}
            onChange={(e) => setInput(e.target.value.toUpperCase())}
            onKeyDown={(e) => e.key === 'Enter' && handleCommand()}
            placeholder={mode === 'estimasi' ? 'cth: 31 45' : 'cth: 31 HB'}
            className="flex-1 min-w-0 bg-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm px-4 py-3 rounded-2xl border border-zinc-700/60 focus:border-teal-500 outline-none transition-colors duration-150"
          />
          <button
            className="w-11 h-11 bg-teal-500 active:bg-teal-600 rounded-2xl flex items-center justify-center text-white shrink-0 transition-colors duration-150"
            onClick={handleCommand}
          >
            <SendIcon />
          </button>
          <button
            className="w-11 h-11 bg-zinc-800 active:bg-zinc-700 rounded-2xl flex items-center justify-center text-zinc-400 active:text-zinc-200 shrink-0 transition-colors duration-150"
            onClick={() => setHistoryOpen(true)}
          >
            <HistoryIcon />
          </button>
        </div>
      </footer>

      {/* ── Overlays ── */}
      <HistoryDrawer
        open={historyOpen}
        onClose={() => setHistoryOpen(false)}
        onEditAkt={(id) => { setHistoryOpen(false); setEditAktId(id) }}
      />
      <SettingsDrawer open={settingsOpen} onClose={() => setSettingsOpen(false)} />
      <EditEstSheet mcNo={editEstMc} onClose={() => setEditEstMc(null)} nowAbs={nowAbs} />
      <EditAktSheet aktualId={editAktId} onClose={() => setEditAktId(null)} />
      <Toast />
      <ConfirmModal />
    </div>
  )
}

function EmptyState({ mode }: { mode: Mode }) {
  return (
    <div className="flex flex-col items-center justify-center h-full gap-3 pb-12 select-none">
      {/* Radar rings — purely decorative */}
      <div className="relative w-24 h-24 flex items-center justify-center">
        <div className="absolute inset-0 rounded-full border border-zinc-800" />
        <div className="absolute inset-3 rounded-full border border-zinc-800/60" />
        <div className="absolute inset-6 rounded-full border border-zinc-800/40" />
        <div className="w-3 h-3 rounded-full bg-zinc-700" />
      </div>
      <p className="text-sm text-zinc-600 text-center leading-relaxed">
        {mode === 'estimasi'
          ? 'Belum ada estimasi aktif'
          : 'Belum ada estimasi aktif'}
      </p>
      <p className="text-xs text-zinc-700 text-center">
        {mode === 'estimasi'
          ? 'Masukkan nomor mesin + menit di bawah'
          : 'Tambah estimasi dulu via mode Estimasi'}
      </p>
    </div>
  )
}
