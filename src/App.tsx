import { useState, useEffect, useRef, useMemo } from 'react'
import { useDoffStore, jamSekarangAbs, jamSekarangLabel } from './store/useDoffStore'
import { useUIStore } from './store/useUIStore'
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
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
    </svg>
  )
}

function HistoryIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
      <line x1="3" y1="6" x2="21" y2="6" />
      <line x1="3" y1="12" x2="21" y2="12" />
      <line x1="3" y1="18" x2="21" y2="18" />
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

  const [historyOpen, setHistoryOpen] = useState(false)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [editEstMc, setEditEstMc] = useState<string | null>(null)
  const [editAktId, setEditAktId] = useState<number | null>(null)

  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    const id = setInterval(() => {
      setClock(jamSekarangLabel())
      setNowAbs(jamSekarangAbs())
    }, 5000)
    return () => clearInterval(id)
  }, [])

  // Shift progress: unique machines doffed + in estimasi
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

  const handleCommand = () => {
    const cmd = input.trim().toUpperCase()
    if (!cmd) return

    let result
    if (mode === 'estimasi') {
      result = store.prosesBarisKondisiMesin(cmd, jamSekarangAbs())
    } else {
      result = store.prosesBarisUmum(cmd)
    }

    if (result.type === 'ok') {
      showToast(result.msg, result.undoFn)
      setInput('')
    } else {
      showToast(`⚠ ${result.msg}`)
    }
    inputRef.current?.focus()
  }

  const handleDoff = (mcNo: string) => {
    const result = store.prosesBarisUmum(mcNo)
    if (result.type === 'ok') {
      showToast(result.msg, result.undoFn)
    } else {
      showToast(`⚠ ${result.msg}`)
    }
  }

  const handleHapusEst = (mcNo: string) => {
    const prevEst = store.estimasi[mcNo]
    store.hapusEstimasi(mcNo)
    showToast(`Mc ${mcNo} dihapus`, () => { if (prevEst) store.restoreEstimasi(prevEst) })
  }

  return (
    <div className="h-screen flex flex-col bg-zinc-950 text-zinc-100 overflow-hidden">
      {/* Header */}
      <header className="flex-shrink-0 flex items-center gap-3 px-4 h-12 border-b border-zinc-800/80">
        <span className="font-black text-base text-zinc-100 tracking-tight">
          Adoel<span className="text-teal-400">.</span>
        </span>
        <span className="flex-1 text-center text-sm font-semibold text-zinc-300 tabular-nums">{clock}</span>
        <div className="flex items-center gap-3">
          <span className="text-xs text-zinc-500 tabular-nums">
            <span className="text-zinc-300 font-semibold">{store.aktual.length}</span>
            <span className="text-zinc-700">/</span>
            <span>{allTouched.size}</span>
          </span>
          <button
            className="w-8 h-8 flex items-center justify-center text-zinc-500 active:text-zinc-200"
            onClick={() => setSettingsOpen(true)}
          >
            <GearIcon />
          </button>
        </div>
      </header>

      {/* Main: radar cards */}
      <main className="flex-1 overflow-y-auto hide-scroll py-2 px-3">
        {radarList.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full gap-2 text-zinc-700 pb-8">
            <span className="text-5xl font-black text-zinc-800">○</span>
            <span className="text-sm">Belum ada estimasi aktif</span>
            <span className="text-xs text-zinc-800">Mode Estimasi → masukkan data mesin</span>
          </div>
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

      {/* Footer: mode toggle + input */}
      <footer className="flex-shrink-0 px-3 pt-2 pb-4 border-t border-zinc-800/80 bg-zinc-950">
        {/* Mode toggle */}
        <div className="flex mb-2 bg-zinc-900 rounded-xl p-0.5 gap-0.5">
          <button
            className={`flex-1 py-1.5 text-xs rounded-xl font-semibold transition-colors ${
              mode === 'estimasi'
                ? 'bg-teal-500 text-white shadow-sm'
                : 'text-zinc-500 active:text-zinc-300'
            }`}
            onClick={() => setMode('estimasi')}
          >
            Estimasi
          </button>
          <button
            className={`flex-1 py-1.5 text-xs rounded-xl font-semibold transition-colors ${
              mode === 'aktual'
                ? 'bg-teal-500 text-white shadow-sm'
                : 'text-zinc-500 active:text-zinc-300'
            }`}
            onClick={() => setMode('aktual')}
          >
            Doff
          </button>
        </div>

        {/* Input row */}
        <div className="flex gap-2">
          <input
            ref={inputRef}
            type="text"
            inputMode="text"
            value={input}
            onChange={(e) => setInput(e.target.value.toUpperCase())}
            onKeyDown={(e) => e.key === 'Enter' && handleCommand()}
            placeholder={mode === 'estimasi' ? 'cth: 31 45' : 'cth: 31 HB'}
            className="flex-1 min-w-0 bg-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm px-4 py-2.5 rounded-2xl border border-zinc-700/60 focus:border-teal-500 outline-none"
          />
          <button
            className="w-11 h-11 bg-teal-500 active:bg-teal-600 rounded-2xl flex items-center justify-center text-white font-black text-lg shrink-0"
            onClick={handleCommand}
          >
            ↑
          </button>
          <button
            className="w-11 h-11 bg-zinc-800 active:bg-zinc-700 rounded-2xl flex items-center justify-center text-zinc-400 shrink-0"
            onClick={() => setHistoryOpen(true)}
          >
            <HistoryIcon />
          </button>
        </div>
      </footer>

      {/* Overlays */}
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
