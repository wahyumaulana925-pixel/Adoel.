import { useRef } from 'react'
import type { Estimasi, MesinData } from '../types'
import { shiftAbsKeJamStr } from '../store/useDoffStore'

interface Props {
  est: Estimasi
  mesin: MesinData | undefined
  nowAbs: number
  onDoff: () => void
  onHapus: () => void
  onLongPress: () => void
}

const REVEAL_W = 160

function urgency(remaining: number) {
  if (remaining > 30) return { border: 'border-teal-500', text: 'text-teal-400', bar: 'bg-teal-500', dim: 'text-teal-600', pulse: false }
  if (remaining > 10) return { border: 'border-amber-400', text: 'text-amber-400', bar: 'bg-amber-400', dim: 'text-amber-600', pulse: false }
  if (remaining > 0)  return { border: 'border-orange-500', text: 'text-orange-400', bar: 'bg-orange-500', dim: 'text-orange-700', pulse: false }
  return { border: 'border-red-500', text: 'text-red-400', bar: 'bg-red-500', dim: 'text-red-700', pulse: true }
}

export function RadarCard({ est, mesin, nowAbs, onDoff, onHapus, onLongPress }: Props) {
  const remaining = est.estAbsMin - nowAbs
  const clr = urgency(remaining)
  const totalDur = est.estAbsMin - est.startAbsMin
  const elapsed = nowAbs - est.startAbsMin
  const progress = totalDur > 0 ? Math.min(1, elapsed / totalDur) : 0
  const remStr = remaining >= 0 ? `+${remaining}m` : `−${Math.abs(remaining)}m`
  const corak = est.corakOverride ?? mesin?.corak ?? '—'
  const tipe = mesin?.tipe ?? '?'

  const innerRef = useRef<HTMLDivElement>(null)
  const startXRef = useRef(0)
  const dragging = useRef(false)
  const revealed = useRef(false)
  const curOffset = useRef(0)
  const longTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const moved = useRef(false)

  const setTranslate = (x: number, animate: boolean) => {
    if (!innerRef.current) return
    innerRef.current.style.transition = animate ? 'transform 0.2s ease' : 'none'
    innerRef.current.style.transform = `translateX(-${x}px)`
    curOffset.current = x
  }

  const snap = (open: boolean) => {
    revealed.current = open
    setTranslate(open ? REVEAL_W : 0, true)
  }

  const handleMoveX = (clientX: number) => {
    const dx = startXRef.current - clientX
    if (!moved.current && Math.abs(dx) > 8) {
      moved.current = true
      if (longTimer.current) { clearTimeout(longTimer.current); longTimer.current = null }
    }
    if (!moved.current) return
    const base = revealed.current ? REVEAL_W : 0
    const offset = Math.max(0, Math.min(REVEAL_W, base + dx))
    setTranslate(offset, false)
  }

  const handleEnd = () => {
    if (longTimer.current) { clearTimeout(longTimer.current); longTimer.current = null }
    if (!dragging.current) return
    dragging.current = false
    if (!moved.current && !revealed.current) return
    snap(curOffset.current > REVEAL_W / 2)
  }

  const startGesture = (clientX: number) => {
    startXRef.current = clientX
    dragging.current = true
    moved.current = false
    longTimer.current = setTimeout(() => {
      longTimer.current = null
      if (!moved.current) {
        dragging.current = false
        if (navigator.vibrate) navigator.vibrate(40)
        onLongPress()
      }
    }, 480)
  }

  const onTouchStart = (e: React.TouchEvent) => startGesture(e.touches[0].clientX)
  const onTouchMove = (e: React.TouchEvent) => handleMoveX(e.touches[0].clientX)
  const onTouchEnd = () => handleEnd()

  const onMouseDown = (e: React.MouseEvent) => {
    startGesture(e.clientX)
    const mm = (ev: MouseEvent) => handleMoveX(ev.clientX)
    const mu = () => { handleEnd(); window.removeEventListener('mousemove', mm); window.removeEventListener('mouseup', mu) }
    window.addEventListener('mousemove', mm)
    window.addEventListener('mouseup', mu)
  }

  return (
    <div className="relative rounded-2xl overflow-hidden" style={{ minHeight: 88 }}>
      {/* Action buttons behind card */}
      <div className="absolute inset-y-0 right-0 flex" style={{ width: REVEAL_W }}>
        <button
          className="flex-1 bg-teal-600 active:bg-teal-500 text-white text-sm font-bold tracking-wide"
          onClick={() => { snap(false); onDoff() }}
        >
          DOFF
        </button>
        <button
          className="w-16 bg-zinc-700 active:bg-zinc-600 text-zinc-300 text-xs font-semibold"
          onClick={() => { snap(false); onHapus() }}
        >
          HAPUS
        </button>
      </div>

      {/* Card face */}
      <div
        ref={innerRef}
        className={`relative bg-zinc-900 border-l-[3px] ${clr.border} rounded-2xl px-4 pt-3 pb-2 select-none ${clr.pulse ? 'animate-pulse-soft' : ''}`}
        onTouchStart={onTouchStart}
        onTouchMove={onTouchMove}
        onTouchEnd={onTouchEnd}
        onMouseDown={onMouseDown}
      >
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <div className="flex items-baseline gap-2">
              <span className="text-xl font-black text-zinc-100 leading-none">
                {est.mcNo}
              </span>
              <span className={`text-[10px] font-semibold uppercase tracking-widest ${clr.dim}`}>
                {tipe}
              </span>
            </div>
            <div className="mt-0.5 text-sm text-zinc-400 truncate">{corak}</div>
          </div>
          <div className="text-right shrink-0">
            <div className={`text-lg font-bold tabular-nums leading-none ${clr.text}`}>
              {shiftAbsKeJamStr(est.estAbsMin)}
            </div>
            <div className={`text-xs tabular-nums mt-0.5 ${remaining < 0 ? 'text-red-400 font-semibold' : 'text-zinc-500'}`}>
              {remStr}
            </div>
          </div>
        </div>

        {/* Progress bar */}
        <div className="mt-2.5 h-1 bg-zinc-800 rounded-full overflow-hidden">
          <div
            className={`h-full rounded-full transition-all duration-1000 ${clr.bar}`}
            style={{ width: `${Math.round(progress * 100)}%` }}
          />
        </div>
      </div>
    </div>
  )
}
