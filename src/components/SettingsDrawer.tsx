import { useState, useRef } from 'react'
import { useDoffStore } from '../store/useDoffStore'
import { useUIStore } from '../store/useUIStore'
import { cancelAllNotifs } from '../lib/notifications'
import type { MesinData, MesinTipe } from '../types'

interface Props {
  open: boolean
  onClose: () => void
}

type Tab = 'edit' | 'list' | 'data'

const TIPES: MesinTipe[] = ['TAPPET', 'CAM', 'D405', 'D408']

function EditMesinTab() {
  const store = useDoffStore()
  const { showToast } = useUIStore()
  const [mcNo, setMcNo] = useState('')
  const [form, setForm] = useState<Partial<MesinData>>({})
  const [loaded, setLoaded] = useState(false)

  const handleLoad = () => {
    const n = mcNo.trim()
    if (!n || !/^\d{1,3}$/.test(n)) { showToast('Nomor mesin tidak valid'); return }
    const mesin = store.db[n]
    if (!mesin) { showToast(`Mc ${n} tidak ditemukan`); return }
    setForm({ ...mesin })
    setLoaded(true)
  }

  const handleSave = () => {
    const n = mcNo.trim()
    if (!form.tipe) return
    const data: MesinData = {
      tipe: form.tipe,
      corak: (form.corak ?? '').trim() || '-',
      targetYard: form.targetYard,
      speed: form.speed,
      koreksi: form.koreksi,
    }
    store.setMesin(n, data)
    showToast(`Mc ${n} disimpan ✓`)
    setLoaded(false)
    setMcNo('')
    setForm({})
  }

  const handleReset = () => {
    store.resetMesin(mcNo.trim())
    showToast(`Mc ${mcNo} direset ke default`)
    setLoaded(false)
    setForm({})
  }

  const onTipeChange = (t: MesinTipe) => {
    setForm((f) => ({
      ...f,
      tipe: t,
      speed: t === 'D405' ? f.speed : undefined,
      koreksi: t === 'D408' ? f.koreksi : undefined,
    }))
  }

  return (
    <div className="space-y-4">
      <div className="flex gap-2">
        <input
          type="number"
          inputMode="numeric"
          value={mcNo}
          onChange={(e) => { setMcNo(e.target.value); setLoaded(false) }}
          onKeyDown={(e) => e.key === 'Enter' && handleLoad()}
          placeholder="Nomor mesin"
          className="flex-1 bg-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm px-4 py-2.5 rounded-xl border border-zinc-700 focus:border-teal-500 outline-none"
        />
        <button
          className="px-4 py-2.5 bg-teal-600 text-white text-sm font-semibold rounded-xl active:bg-teal-700"
          onClick={handleLoad}
        >
          Load
        </button>
      </div>

      {loaded && form.tipe && (
        <div className="space-y-3">
          <div>
            <label className="text-xs text-zinc-500 uppercase tracking-wider mb-1 block">Tipe</label>
            <div className="flex gap-2 flex-wrap">
              {TIPES.map((t) => (
                <button
                  key={t}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition-colors ${
                    form.tipe === t
                      ? 'bg-teal-600 border-teal-500 text-white'
                      : 'border-zinc-600 text-zinc-400 active:bg-zinc-700'
                  }`}
                  onClick={() => onTipeChange(t)}
                >
                  {t}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="text-xs text-zinc-500 uppercase tracking-wider mb-1 block">Corak</label>
            <input
              type="text"
              value={form.corak ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, corak: e.target.value }))}
              placeholder="-"
              className="w-full bg-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm px-4 py-2.5 rounded-xl border border-zinc-700 focus:border-teal-500 outline-none"
            />
          </div>

          {(form.tipe === 'D405' || form.tipe === 'D408' || form.tipe === 'TAPPET' || form.tipe === 'CAM') && (
            <div>
              <label className="text-xs text-zinc-500 uppercase tracking-wider mb-1 block">Target Yard</label>
              <input
                type="number"
                inputMode="decimal"
                value={form.targetYard ?? ''}
                onChange={(e) => setForm((f) => ({ ...f, targetYard: e.target.value ? parseFloat(e.target.value) : undefined }))}
                placeholder="opsional"
                className="w-full bg-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm px-4 py-2.5 rounded-xl border border-zinc-700 focus:border-teal-500 outline-none"
              />
            </div>
          )}

          {form.tipe === 'D405' && (
            <div>
              <label className="text-xs text-zinc-500 uppercase tracking-wider mb-1 block">Speed (yard/menit)</label>
              <input
                type="number"
                inputMode="decimal"
                value={form.speed ?? ''}
                onChange={(e) => setForm((f) => ({ ...f, speed: e.target.value ? parseFloat(e.target.value) : undefined }))}
                placeholder="cth: 0.158"
                className="w-full bg-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm px-4 py-2.5 rounded-xl border border-zinc-700 focus:border-teal-500 outline-none"
              />
            </div>
          )}

          {form.tipe === 'D408' && (
            <div>
              <label className="text-xs text-zinc-500 uppercase tracking-wider mb-1 block">Koreksi (menit)</label>
              <input
                type="number"
                inputMode="decimal"
                value={form.koreksi ?? ''}
                onChange={(e) => setForm((f) => ({ ...f, koreksi: e.target.value ? parseFloat(e.target.value) : undefined }))}
                placeholder="cth: 18"
                className="w-full bg-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm px-4 py-2.5 rounded-xl border border-zinc-700 focus:border-teal-500 outline-none"
              />
            </div>
          )}

          <div className="flex gap-2 pt-1">
            <button
              className="px-4 py-2.5 rounded-xl border border-zinc-600 text-zinc-400 text-sm active:bg-zinc-800"
              onClick={handleReset}
            >
              Reset
            </button>
            <button
              className="flex-1 py-2.5 rounded-xl bg-teal-500 text-white text-sm font-semibold active:bg-teal-600"
              onClick={handleSave}
            >
              Simpan
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

function DaftarMesinTab() {
  const store = useDoffStore()
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState<MesinTipe | 'ALL'>('ALL')

  const entries = Object.entries(store.db)
    .filter(([k, v]) => {
      if (filter !== 'ALL' && v.tipe !== filter) return false
      if (search && !k.includes(search) && !v.corak.toLowerCase().includes(search.toLowerCase())) return false
      return v.corak && v.corak !== '-'
    })
    .sort(([a], [b]) => parseInt(a) - parseInt(b))

  return (
    <div className="space-y-3">
      <input
        type="text"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        placeholder="Cari nomor / corak..."
        className="w-full bg-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm px-4 py-2.5 rounded-xl border border-zinc-700 focus:border-teal-500 outline-none"
      />
      <div className="flex gap-1.5 flex-wrap">
        {(['ALL', ...TIPES] as const).map((t) => (
          <button
            key={t}
            className={`px-3 py-1 rounded-lg text-xs font-semibold border transition-colors ${
              filter === t
                ? 'bg-teal-600 border-teal-500 text-white'
                : 'border-zinc-700 text-zinc-500 active:bg-zinc-800'
            }`}
            onClick={() => setFilter(t)}
          >
            {t === 'ALL' ? 'Semua' : t}
          </button>
        ))}
      </div>
      <div className="space-y-1 max-h-64 overflow-y-auto hide-scroll">
        {entries.map(([k, v]) => (
          <div key={k} className="flex items-center gap-3 px-3 py-2 bg-zinc-800/50 rounded-xl">
            <span className="font-bold text-zinc-200 text-sm w-8 tabular-nums">{k}</span>
            <span className="text-xs text-zinc-500 uppercase tracking-wide w-14">{v.tipe}</span>
            <span className="flex-1 text-sm text-zinc-300 truncate">{v.corak}</span>
            {v.targetYard && <span className="text-xs text-zinc-600">{v.targetYard}y</span>}
          </div>
        ))}
        {entries.length === 0 && (
          <p className="text-center text-zinc-600 text-sm py-6">Tidak ditemukan</p>
        )}
      </div>
    </div>
  )
}

function DataTab() {
  const store = useDoffStore()
  const { showToast, showConfirm } = useUIStore()
  const fileRef = useRef<HTMLInputElement>(null)

  const handleExport = async () => {
    const data = JSON.stringify({ db: store.db, estimasi: store.estimasi, aktual: store.aktual }, null, 2)
    if (navigator.share) {
      const blob = new Blob([data], { type: 'application/json' })
      const file = new File([blob], 'adoel-backup.json', { type: 'application/json' })
      try { await navigator.share({ files: [file], title: 'Adoel Backup' }) } catch { /* cancelled */ }
    } else {
      await navigator.clipboard.writeText(data)
      showToast('Data disalin ke clipboard')
    }
  }

  const handleImport = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (ev) => {
      try {
        const parsed = JSON.parse(ev.target?.result as string)
        if (parsed.db) store.setMesin && Object.entries(parsed.db).forEach(([k, v]) => store.setMesin(k, v as MesinData))
        showToast('Data berhasil diimpor ✓')
      } catch {
        showToast('⚠ File tidak valid')
      }
    }
    reader.readAsText(file)
    e.target.value = ''
  }

  const handleReset = () => {
    showConfirm('Reset semua data ke default? Estimasi & riwayat akan hilang.', async () => {
      // Cancel all pending notifications before wiping estimasi
      await cancelAllNotifs(Object.keys(store.estimasi))
      store.resetDb()
      showToast('Data direset ke default')
    })
  }

  return (
    <div className="space-y-3">
      <button
        className="w-full py-3 rounded-2xl border border-zinc-700 text-zinc-300 text-sm font-medium active:bg-zinc-800"
        onClick={handleExport}
      >
        Export / Bagikan Data
      </button>
      <button
        className="w-full py-3 rounded-2xl border border-zinc-700 text-zinc-300 text-sm font-medium active:bg-zinc-800"
        onClick={() => fileRef.current?.click()}
      >
        Import dari File
      </button>
      <input ref={fileRef} type="file" accept=".json" className="hidden" onChange={handleImport} />
      <button
        className="w-full py-3 rounded-2xl border border-red-800 text-red-400 text-sm font-medium active:bg-red-950"
        onClick={handleReset}
      >
        Reset ke Default
      </button>
    </div>
  )
}

export function SettingsDrawer({ open, onClose }: Props) {
  const [tab, setTab] = useState<Tab>('edit')

  if (!open) return null

  return (
    <>
      <div className="fixed inset-0 bg-black/60 z-30" onClick={onClose} />
      <div
        className="fixed bottom-0 left-0 right-0 z-30 flex flex-col bg-zinc-950 border-t border-zinc-800 rounded-t-3xl"
        style={{ maxHeight: '90vh' }}
      >
        <div className="flex-shrink-0 px-5 pt-4 pb-3">
          <div className="w-10 h-1 bg-zinc-700 rounded-full mx-auto mb-4" />
          <div className="flex items-center justify-between mb-3">
            <span className="font-bold text-zinc-100">Pengaturan</span>
            <button className="text-zinc-500 text-sm" onClick={onClose}>✕</button>
          </div>
          <div className="flex bg-zinc-900 rounded-xl p-0.5">
            {([['edit', 'Edit Mesin'], ['list', 'Daftar'], ['data', 'Data']] as const).map(([t, label]) => (
              <button
                key={t}
                className={`flex-1 py-2 text-xs rounded-lg font-semibold transition-colors ${
                  tab === t ? 'bg-teal-500 text-white' : 'text-zinc-400'
                }`}
                onClick={() => setTab(t)}
              >
                {label}
              </button>
            ))}
          </div>
        </div>

        <div className="flex-1 overflow-y-auto hide-scroll px-5 pb-safe-floor">
          {tab === 'edit' && <EditMesinTab />}
          {tab === 'list' && <DaftarMesinTab />}
          {tab === 'data' && <DataTab />}
        </div>
      </div>
    </>
  )
}
