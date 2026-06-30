import { useState, useEffect, useRef } from 'react'
import { useDoffStore, jamSekarangAbs, shiftAbsKeJamStr, standarisasiKeterangan } from '../store/useDoffStore'
import { useUIStore } from '../store/useUIStore'
import { scheduleNotif, cancelNotif } from '../lib/notifications'

interface Props {
  mcNo: string | null
  onClose: () => void
  nowAbs: number
}

const INPUT_CLS = 'w-full bg-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm px-4 py-3 rounded-xl border border-zinc-700/80 focus:border-teal-500 outline-none transition-colors duration-150'
const LABEL_CLS = 'text-xs text-zinc-500 uppercase tracking-wider mb-1.5 block font-semibold'

export function EditEstSheet({ mcNo, onClose, nowAbs }: Props) {
  const store = useDoffStore()
  const { showToast } = useUIStore()
  const inputRef = useRef<HTMLInputElement>(null)
  const [val, setVal] = useState('')
  const [corakVal, setCorakVal] = useState('')

  const est   = mcNo ? store.estimasi[mcNo] : null
  const mesin = mcNo ? store.db[mcNo]       : null

  useEffect(() => {
    if (mcNo && est) {
      setVal('')
      setCorakVal(est.corakOverride ?? mesin?.corak ?? '')
      setTimeout(() => inputRef.current?.focus(), 100)
    }
  }, [mcNo])

  if (!mcNo || !est || !mesin) return null

  const delta    = est.estAbsMin - nowAbs
  const deltaStr = delta >= 0 ? `+${delta}m` : `−${Math.abs(delta)}m`

  const handleSave = async () => {
    const raw = val.trim().toUpperCase()
    let newEstAbs: number | undefined
    if (raw) {
      const result = store.prosesBarisKondisiMesin(`${mcNo} ${raw}`, jamSekarangAbs())
      if (result.type === 'err') { showToast(`⚠ ${result.msg}`); return }
      newEstAbs = result.estAbs
    }
    const corak = standarisasiKeterangan(corakVal.trim())
    if (corak && corak !== mesin.corak) {
      store.updateEstimasi(mcNo, { corakOverride: corak })
    } else {
      store.updateEstimasi(mcNo, { corakOverride: undefined })
    }
    const finalEst    = store.estimasi[mcNo]
    const finalAbsMin = newEstAbs ?? finalEst?.estAbsMin
    if (finalAbsMin) await scheduleNotif(mcNo, finalAbsMin)
    showToast(`Mc ${mcNo} diperbarui`)
    onClose()
  }

  const handleHapus = async () => {
    const prevEst = est
    store.hapusEstimasi(mcNo)
    await cancelNotif(mcNo)
    showToast(`Mc ${mcNo} dihapus`, async () => {
      store.restoreEstimasi(prevEst)
      await scheduleNotif(prevEst.mcNo, prevEst.estAbsMin)
    })
    onClose()
  }

  const inputHint =
    mesin.tipe === 'TAPPET' || mesin.tipe === 'CAM' ? 'menit'       :
    mesin.tipe === 'D405'                             ? 'yard'        : 'jam counter'
  const inputPlaceholder =
    mesin.tipe === 'D408'  ? 'cth: 14.30' :
    mesin.tipe === 'D405'  ? 'cth: 150y'  : 'cth: 45'

  return (
    <>
      <div className="fixed inset-0 bg-black/60 z-40 animate-fade-in" onClick={onClose} />
      <div className="fixed bottom-0 left-0 right-0 z-40 bg-zinc-900 border-t border-zinc-800 rounded-t-3xl px-5 pt-5 pb-safe-floor animate-slide-up">
        <div className="w-10 h-1 bg-zinc-700 rounded-full mx-auto mb-5" />

        {/* Header: machine identity + current estimate */}
        <div className="flex items-center justify-between mb-5">
          <div>
            <span className="text-xl font-black text-zinc-100 tabular-nums">Mc {mcNo}</span>
            <span className="ml-2 text-xs text-zinc-500 uppercase tracking-widest font-semibold">{mesin.tipe}</span>
          </div>
          <div className="text-right">
            <div className="text-lg font-black text-teal-400 tabular-nums leading-none">{shiftAbsKeJamStr(est.estAbsMin)}</div>
            <div className={`text-xs tabular-nums mt-0.5 font-semibold ${delta < 0 ? 'text-red-400' : 'text-zinc-500'}`}>{deltaStr}</div>
          </div>
        </div>

        {/* Fields */}
        <div className="space-y-3.5 mb-5">
          <div>
            <label className={LABEL_CLS}>Estimasi baru ({inputHint})</label>
            <input
              ref={inputRef}
              type="text"
              inputMode="text"
              value={val}
              onChange={(e) => setVal(e.target.value.toUpperCase())}
              onKeyDown={(e) => e.key === 'Enter' && handleSave()}
              placeholder={inputPlaceholder}
              className={INPUT_CLS}
            />
          </div>
          <div>
            <label className={LABEL_CLS}>Corak (override sesi ini)</label>
            <input
              type="text"
              value={corakVal}
              onChange={(e) => setCorakVal(e.target.value)}
              placeholder={mesin.corak}
              className={INPUT_CLS}
            />
          </div>
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
