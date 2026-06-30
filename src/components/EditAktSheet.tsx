import { useState, useEffect, useRef } from 'react'
import { useDoffStore } from '../store/useDoffStore'
import { useUIStore } from '../store/useUIStore'

interface Props {
  aktualId: number | null
  onClose: () => void
}

const INPUT_CLS = 'w-full bg-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm px-4 py-3 rounded-xl border border-zinc-700/80 focus:border-teal-500 outline-none transition-colors duration-150'
const LABEL_CLS = 'text-xs text-zinc-500 uppercase tracking-wider mb-1.5 block font-semibold'

export function EditAktSheet({ aktualId, onClose }: Props) {
  const store = useDoffStore()
  const { showToast, showConfirm } = useUIStore()
  const inputRef = useRef<HTMLInputElement>(null)
  const [val, setVal] = useState('')

  const entry = aktualId !== null ? store.aktual.find((a) => a.id === aktualId) : null

  useEffect(() => {
    if (entry) {
      setVal(entry.ket)
      setTimeout(() => inputRef.current?.focus(), 100)
    }
  }, [aktualId])

  if (!entry) return null

  const mesin = store.db[entry.mcNo]
  const corak = entry.corakOverride ?? mesin?.corak ?? '—'

  const handleSave = () => {
    const newKet = val.trim()
    if (!newKet) return
    store.updateAktual(entry.id, { ket: newKet })
    showToast('Riwayat diperbarui')
    onClose()
  }

  const handleHapus = () => {
    showConfirm(`Hapus doff Mc ${entry.mcNo}?`, () => {
      store.hapusAktualById(entry.id)
      showToast(`Mc ${entry.mcNo} dihapus`, () => store.restoreAktual(entry))
      onClose()
    })
  }

  return (
    <>
      <div className="fixed inset-0 bg-black/60 z-40 animate-fade-in" onClick={onClose} />
      <div className="fixed bottom-0 left-0 right-0 z-40 bg-zinc-900 border-t border-zinc-800 rounded-t-3xl px-5 pt-5 pb-safe-floor animate-slide-up">
        <div className="w-10 h-1 bg-zinc-700 rounded-full mx-auto mb-5" />

        {/* Header */}
        <div className="flex items-center gap-3 mb-5">
          <span className="text-xl font-black text-zinc-100 tabular-nums">Mc {entry.mcNo}</span>
          <span className="text-sm text-zinc-400 truncate flex-1">{corak}</span>
          <span className="text-sm text-zinc-500 tabular-nums shrink-0 font-medium">{entry.jam}</span>
        </div>

        {/* Field */}
        <div className="mb-5">
          <label className={LABEL_CLS}>Keterangan</label>
          <input
            ref={inputRef}
            type="text"
            value={val}
            onChange={(e) => setVal(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSave()}
            className={INPUT_CLS}
          />
        </div>

        {/* Actions */}
        <div className="flex gap-2.5">
          <button
            className="py-3 px-4 rounded-2xl border border-zinc-700 text-red-400 text-sm font-medium active:bg-zinc-800 transition-colors duration-150"
            onClick={handleHapus}
          >
            Hapus
          </button>
          <button
            className="flex-1 py-3 rounded-2xl border border-zinc-700 text-zinc-400 text-sm font-medium active:bg-zinc-800 transition-colors duration-150"
            onClick={onClose}
          >
            Batal
          </button>
          <button
            className="flex-1 py-3 rounded-2xl bg-teal-500 text-white text-sm font-semibold active:bg-teal-600 transition-colors duration-150"
            onClick={handleSave}
          >
            Simpan
          </button>
        </div>
      </div>
    </>
  )
}
