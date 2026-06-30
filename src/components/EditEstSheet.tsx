import { useState, useEffect, useRef } from 'react'
import { useDoffStore, jamSekarangAbs, shiftAbsKeJamStr, standarisasiKeterangan } from '../store/useDoffStore'
import { useUIStore } from '../store/useUIStore'

interface Props {
  mcNo: string | null
  onClose: () => void
  nowAbs: number
}

export function EditEstSheet({ mcNo, onClose, nowAbs }: Props) {
  const store = useDoffStore()
  const { showToast } = useUIStore()
  const inputRef = useRef<HTMLInputElement>(null)
  const [val, setVal] = useState('')
  const [corakVal, setCorakVal] = useState('')

  const est = mcNo ? store.estimasi[mcNo] : null
  const mesin = mcNo ? store.db[mcNo] : null

  useEffect(() => {
    if (mcNo && est) {
      setVal('')
      setCorakVal(est.corakOverride ?? mesin?.corak ?? '')
      setTimeout(() => inputRef.current?.focus(), 100)
    }
  }, [mcNo])

  if (!mcNo || !est || !mesin) return null

  const handleSave = () => {
    const raw = val.trim().toUpperCase()
    if (raw) {
      const result = store.prosesBarisKondisiMesin(`${mcNo} ${raw}`, jamSekarangAbs())
      if (result.type === 'err') { showToast(`⚠ ${result.msg}`); return }
    }
    const corak = standarisasiKeterangan(corakVal.trim())
    if (corak && corak !== (mesin.corak)) {
      store.updateEstimasi(mcNo, { corakOverride: corak })
    } else {
      store.updateEstimasi(mcNo, { corakOverride: undefined })
    }
    showToast(`Mc ${mcNo} diperbarui`)
    onClose()
  }

  const handleHapus = () => {
    const prevEst = est
    store.hapusEstimasi(mcNo)
    showToast(`Mc ${mcNo} dihapus`, () => store.restoreEstimasi(prevEst))
    onClose()
  }

  return (
    <>
      <div className="fixed inset-0 bg-black/60 z-40" onClick={onClose} />
      <div className="fixed bottom-0 left-0 right-0 z-40 bg-zinc-900 border-t border-zinc-700 rounded-t-3xl px-5 pt-5 pb-8">
        <div className="w-10 h-1 bg-zinc-700 rounded-full mx-auto mb-5" />

        <div className="flex items-center justify-between mb-4">
          <div>
            <span className="text-lg font-black text-zinc-100">Mc {mcNo}</span>
            <span className="ml-2 text-xs text-zinc-500 uppercase tracking-widest">{mesin.tipe}</span>
          </div>
          <div className="text-right">
            <div className="text-base font-bold text-teal-400 tabular-nums">{shiftAbsKeJamStr(est.estAbsMin)}</div>
            <div className="text-xs text-zinc-500">{est.estAbsMin - nowAbs >= 0 ? `+${est.estAbsMin - nowAbs}m` : `−${nowAbs - est.estAbsMin}m`}</div>
          </div>
        </div>

        <div className="space-y-3 mb-5">
          <div>
            <label className="text-xs text-zinc-500 uppercase tracking-wider mb-1 block">
              Estimasi baru ({mesin.tipe === 'TAPPET' || mesin.tipe === 'CAM' ? 'menit' : mesin.tipe === 'D405' ? 'yard' : 'jam counter'})
            </label>
            <input
              ref={inputRef}
              type="text"
              inputMode="text"
              value={val}
              onChange={(e) => setVal(e.target.value.toUpperCase())}
              onKeyDown={(e) => e.key === 'Enter' && handleSave()}
              placeholder={mesin.tipe === 'D408' ? 'cth: 14.30' : mesin.tipe === 'D405' ? 'cth: 150y' : 'cth: 45'}
              className="w-full bg-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm px-4 py-3 rounded-xl border border-zinc-700 focus:border-teal-500 outline-none"
            />
          </div>
          <div>
            <label className="text-xs text-zinc-500 uppercase tracking-wider mb-1 block">Corak (override sesi ini)</label>
            <input
              type="text"
              value={corakVal}
              onChange={(e) => setCorakVal(e.target.value)}
              placeholder={mesin.corak}
              className="w-full bg-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm px-4 py-3 rounded-xl border border-zinc-700 focus:border-teal-500 outline-none"
            />
          </div>
        </div>

        <div className="flex gap-3">
          <button
            className="py-3 px-5 rounded-2xl border border-zinc-600 text-red-400 text-sm font-medium active:bg-zinc-800"
            onClick={handleHapus}
          >
            Hapus
          </button>
          <button
            className="flex-1 py-3 rounded-2xl border border-zinc-600 text-zinc-400 text-sm font-medium active:bg-zinc-800"
            onClick={onClose}
          >
            Batal
          </button>
          <button
            className="flex-1 py-3 rounded-2xl bg-teal-500 text-white text-sm font-semibold active:bg-teal-600"
            onClick={handleSave}
          >
            Simpan
          </button>
        </div>
      </div>
    </>
  )
}
