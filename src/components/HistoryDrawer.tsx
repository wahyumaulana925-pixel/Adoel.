import { useRef } from 'react'
import { useDoffStore } from '../store/useDoffStore'
import { useUIStore } from '../store/useUIStore'
import type { AktualEntry } from '../types'

interface Props {
  open: boolean
  onClose: () => void
  onEditAkt: (id: number) => void
}

const REVEAL_W = 150

const TIPE_DOT: Record<string, string> = {
  TAPPET: 'bg-teal-500',
  CAM:    'bg-violet-500',
  D405:   'bg-amber-500',
  D408:   'bg-sky-500',
}

function HistoryRow({
  entry, index, total, onEdit, onHapus,
}: {
  entry: AktualEntry
  index: number
  total: number
  onEdit: () => void
  onHapus: () => void
}) {
  const store = useDoffStore()
  const mesin = store.db[entry.mcNo]
  const corak = entry.corakOverride ?? mesin?.corak ?? '—'
  const dot   = TIPE_DOT[mesin?.tipe ?? ''] ?? 'bg-zinc-600'

  const innerRef  = useRef<HTMLDivElement>(null)
  const startXRef = useRef(0)
  const dragging  = useRef(false)
  const revealed  = useRef(false)
  const curOffset = useRef(0)
  const moved     = useRef(false)

  const setTranslate = (x: number, animate: boolean) => {
    if (!innerRef.current) return
    innerRef.current.style.transition = animate ? 'transform 0.22s cubic-bezier(0.32,0.72,0,1)' : 'none'
    innerRef.current.style.transform  = `translateX(-${x}px)`
    curOffset.current = x
  }
  const snap = (open: boolean) => { revealed.current = open; setTranslate(open ? REVEAL_W : 0, true) }

  const handleMoveX = (clientX: number) => {
    const dx = startXRef.current - clientX
    if (!moved.current && Math.abs(dx) > 8) moved.current = true
    if (!moved.current) return
    const base = revealed.current ? REVEAL_W : 0
    setTranslate(Math.max(0, Math.min(REVEAL_W, base + dx)), false)
  }
  const handleEnd = () => {
    if (!dragging.current) return
    dragging.current = false
    snap(curOffset.current > REVEAL_W / 2)
  }
  const handleCancel = () => { dragging.current = false; snap(false) }
  const startGesture = (clientX: number) => { startXRef.current = clientX; dragging.current = true; moved.current = false }

  const onTouchStart  = (e: React.TouchEvent) => startGesture(e.touches[0].clientX)
  const onTouchMove   = (e: React.TouchEvent) => handleMoveX(e.touches[0].clientX)
  const onTouchEnd    = () => handleEnd()
  const onTouchCancel = () => handleCancel()
  const onMouseDown  = (e: React.MouseEvent) => {
    startGesture(e.clientX)
    const mm = (ev: MouseEvent) => handleMoveX(ev.clientX)
    const mu = () => { handleEnd(); window.removeEventListener('mousemove', mm); window.removeEventListener('mouseup', mu) }
    window.addEventListener('mousemove', mm)
    window.addEventListener('mouseup', mu)
  }

  const num = total - index

  return (
    <div className="relative overflow-hidden rounded-2xl bg-zinc-900 isolate">
      {/* Action buttons — EDIT (primary, cyan) + HAPUS (red) */}
      <div className="absolute inset-y-0 right-0 flex" style={{ width: REVEAL_W }}>
        <button
          className="flex-1 flex items-center justify-center bg-cyan-700 active:bg-cyan-600 text-white text-xs font-bold tracking-wide rounded-l-2xl"
          onClick={() => { snap(false); onEdit() }}
        >
          EDIT
        </button>
        <div className="w-px bg-black/20" />
        <button
          className="w-[65px] flex items-center justify-center bg-red-700/90 active:bg-red-700 text-white text-xs font-bold tracking-wide rounded-r-2xl"
          onClick={() => { snap(false); onHapus() }}
        >
          HAPUS
        </button>
      </div>

      {/* Row face */}
      <div
        ref={innerRef}
        className="absolute inset-0 bg-zinc-900 flex items-center gap-3 px-4 py-3 select-none z-10"
        onTouchStart={onTouchStart}
        onTouchMove={onTouchMove}
        onTouchEnd={onTouchEnd}
        onTouchCancel={onTouchCancel}
        onMouseDown={onMouseDown}
      >
        {/* Sequence number */}
        <span className="text-sm font-black text-zinc-500 tabular-nums w-5 text-right shrink-0">
          {num}
        </span>

        {/* Type dot */}
        <span className={`w-2 h-2 rounded-full shrink-0 ${dot}`} />

        {/* Machine number + corak */}
        <div className="min-w-0 flex-1">
          <div className="text-3xl font-black tracking-tighter tabular-nums leading-none text-cyan-500">
            {entry.mcNo}
          </div>
          <div className="text-[11px] font-bold uppercase tracking-wide text-zinc-500 truncate mt-0.5">
            {corak}{entry.customYard !== undefined ? ` · ${entry.customYard}y` : ''}
          </div>
        </div>

        {/* Ket / condition */}
        <span className="text-sm font-bold text-zinc-200 tabular-nums shrink-0">{entry.ket}</span>
      </div>
    </div>
  )
}

export function HistoryDrawer({ open, onClose, onEditAkt }: Props) {
  const store = useDoffStore()
  const { showToast, showConfirm } = useUIStore()

  if (!open) return null

  const chronological = [...store.aktual].reverse()
  const total = chronological.length

  const handleShare = async () => {
    const today   = new Date()
    const dateStr = `${today.getDate().toString().padStart(2, '0')}/${(today.getMonth() + 1).toString().padStart(2, '0')}/${today.getFullYear()}`
    const lines   = chronological.map((a, i) => {
      const mesin  = store.db[a.mcNo]
      const corak  = a.corakOverride ?? mesin?.corak ?? '—'
      const suffix = a.customYard !== undefined ? ` [${a.customYard}y]` : ''
      return `${i + 1}. Mc${a.mcNo} - ${corak}${suffix} - ${a.ket}`
    })
    const text = `Adoel V5\n${dateStr}\n\n${lines.join('\n')}\n\nTotal: ${total} doff`

    if (navigator.share) {
      try { await navigator.share({ text }) } catch { /* user cancelled */ }
    } else {
      await navigator.clipboard.writeText(text)
      showToast('Riwayat disalin ke clipboard')
    }
  }

  const handleFinishShift = () => {
    showConfirm(
      `Akhiri shift? Riwayat ${total} doff akan dihapus.`,
      () => { store.finishShift(); onClose(); showToast('Shift selesai ✓') }
    )
  }

  const handleHapus = (entry: AktualEntry) => {
    showConfirm(`Hapus doff Mc ${entry.mcNo}?`, () => {
      store.hapusAktualById(entry.id)
      showToast(`Mc ${entry.mcNo} dihapus`, () => store.restoreAktual(entry))
    })
  }

  return (
    <>
      <div className="fixed inset-0 bg-black/60 z-30 animate-fade-in" onClick={onClose} />
      <div
        className="fixed bottom-0 left-0 right-0 z-30 flex flex-col bg-zinc-950 border-t border-zinc-800 rounded-t-3xl animate-slide-up"
        style={{ maxHeight: '80vh' }}
      >
        {/* Handle + header */}
        <div className="flex-shrink-0 px-5 pt-4 pb-3">
          <div className="w-10 h-1 bg-zinc-700 rounded-full mx-auto mb-4" />
          <div className="flex items-center justify-between">
            <span className="font-bold text-zinc-100 text-base">Riwayat</span>
            <span className="text-sm font-bold text-cyan-500 tabular-nums">{total} doff</span>
          </div>
        </div>

        {/* Scrollable list */}
        <div className="flex-1 overflow-y-auto hide-scroll px-4 pb-2">
          {total === 0 ? (
            <div className="text-center text-zinc-600 text-sm py-12">Belum ada doff</div>
          ) : (
            <div className="flex flex-col gap-1.5">
              {chronological.map((entry, i) => (
                <HistoryRow
                  key={entry.id}
                  entry={entry}
                  index={i}
                  total={total}
                  onEdit={() => onEditAkt(entry.id)}
                  onHapus={() => handleHapus(entry)}
                />
              ))}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex-shrink-0 flex gap-3 px-4 pt-3 pb-safe-floor border-t border-zinc-800">
          <button
            className="flex-1 py-3 rounded-2xl border border-zinc-700 text-zinc-400 text-sm font-medium active:bg-zinc-800 transition-colors duration-150"
            onClick={handleShare}
          >
            Bagikan
          </button>
          <button
            className="flex-1 py-3 rounded-2xl bg-cyan-600 active:bg-cyan-700 text-white text-sm font-semibold transition-colors duration-150"
            onClick={handleFinishShift}
          >
            Selesai Shift
          </button>
        </div>
      </div>
    </>
  )
}
