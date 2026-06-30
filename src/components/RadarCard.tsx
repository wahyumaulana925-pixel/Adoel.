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

// Width revealed on full left-swipe
const REVEAL_W = 156

function urgency(remaining: number) {
  if (remaining > 30) return {
    border: 'border-teal-500',
    text:   'text-teal-400',
    bar:    'bg-teal-500',
    label:  'text-teal-600',
    cardBg: '',
    pulse:  false,
  }
  if (remaining > 10) return {
    border: 'border-amber-400',
    text:   'text-amber-400',
    bar:    'bg-amber-400',
    label:  'text-amber-600',
    cardBg: '',
    pulse:  false,
  }
  if (remaining > 0) return {
    border: 'border-orange-500',
    text:   'text-orange-400',
    bar:    'bg-orange-500',
    label:  'text-orange-600',
    cardBg: '',
    pulse:  false,
  }
  return {
    border: 'border-red-500',
    text:   'text-red-400',
    bar:    'bg-red-500',
    label:  'text-red-600',
    cardBg: 'bg-red-950/20',
    pulse:  true,
  }
}

export function RadarCard({ est, mesin, nowAbs, onDoff, onHapus, onLongPress }: Props) {
  const remaining = est.estAbsMin - nowAbs
  const clr       = urgency(remaining)
  const totalDur  = est.estAbsMin - est.startAbsMin
  const elapsed   = nowAbs - est.startAbsMin
  const progress  = totalDur > 0 ? Math.min(1, Math.max(0, elapsed / totalDur)) : 0
  const remStr    = remaining >= 0 ? `+${remaining}m` : `−${Math.abs(remaining)}m`
  const corak     = est.corakOverride ?? mesin?.corak ?? '—'
  const tipe      = mesin?.tipe ?? '?'

  // Swipe gesture refs — mutations, not state, so no re-renders during drag
  const innerRef  = useRef<HTMLDivElement>(null)
  const startXRef = useRef(0)
  const dragging  = useRef(false)
  const revealed  = useRef(false)
  const curOffset = useRef(0)
  const moved     = useRef(false)
  const longTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const setTranslate = (x: number, animate: boolean) => {
    if (!innerRef.current) return
    innerRef.current.style.transition = animate ? 'transform 0.22s cubic-bezier(0.32,0.72,0,1)' : 'none'
    innerRef.current.style.transform  = `translateX(-${x}px)`
    curOffset.current = x
  }
  const snap = (open: boolean) => { revealed.current = open; setTranslate(open ? REVEAL_W : 0, true) }

  const handleMoveX = (clientX: number) => {
    const dx = startXRef.current - clientX
    if (!moved.current && Math.abs(dx) > 6) {
      moved.current = true
      if (longTimer.current) { clearTimeout(longTimer.current); longTimer.current = null }
    }
    if (!moved.current) return
    const base = revealed.current ? REVEAL_W : 0
    setTranslate(Math.max(0, Math.min(REVEAL_W, base + dx)), false)
  }
  const handleEnd = () => {
    if (longTimer.current) { clearTimeout(longTimer.current); longTimer.current = null }
    if (!dragging.current) return
    dragging.current = false
    snap(curOffset.current > REVEAL_W / 2)
  }
  const startGesture = (clientX: number) => {
    startXRef.current = clientX
    dragging.current  = true
    moved.current     = false
    longTimer.current = setTimeout(() => {
      longTimer.current = null
      if (!moved.current) {
        dragging.current = false
        navigator.vibrate?.(40)
        onLongPress()
      }
    }, 480)
  }

  const onTouchStart = (e: React.TouchEvent) => startGesture(e.touches[0].clientX)
  const onTouchMove  = (e: React.TouchEvent) => handleMoveX(e.touches[0].clientX)
  const onTouchEnd   = () => handleEnd()
  const onMouseDown  = (e: React.MouseEvent) => {
    startGesture(e.clientX)
    const mm = (ev: MouseEvent) => handleMoveX(ev.clientX)
    const mu = () => { handleEnd(); window.removeEventListener('mousemove', mm); window.removeEventListener('mouseup', mu) }
    window.addEventListener('mousemove', mm)
    window.addEventListener('mouseup', mu)
  }

  return (
    <div className="relative rounded-2xl overflow-hidden">
      {/* Action strip — revealed on left-swipe */}
      <div className="absolute inset-y-0 right-0 flex" style={{ width: REVEAL_W }}>
        <button
          className="flex-1 flex items-center justify-center bg-teal-600 active:bg-teal-500 text-white"
          onClick={() => { snap(false); onDoff() }}
        >
          <span className="text-sm font-black tracking-widest">DOFF</span>
        </button>
        <div className="w-px bg-black/25" />
        <button
          className="w-14 flex items-center justify-center bg-zinc-700 active:bg-zinc-600 text-zinc-300"
          onClick={() => { snap(false); onHapus() }}
        >
          <span className="text-[10px] font-bold tracking-widest">HAPUS</span>
        </button>
      </div>

      {/* Card face — transition-colors lets urgency tier shifts animate smoothly */}
      <div
        ref={innerRef}
        className={[
          'relative rounded-2xl border-l-[3px] select-none bg-zinc-900 transition-colors duration-700',
          clr.border,
          clr.cardBg,
          clr.pulse ? 'animate-pulse-soft' : '',
        ].filter(Boolean).join(' ')}
        onTouchStart={onTouchStart}
        onTouchMove={onTouchMove}
        onTouchEnd={onTouchEnd}
        onMouseDown={onMouseDown}
      >
        <div className="px-4 pt-3.5 pb-3">
          {/* Row 1: machine number + tipe   |   est. time + delta */}
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <div className="flex items-baseline gap-2">
                <span className="text-2xl font-black text-zinc-100 leading-none tabular-nums">
                  {est.mcNo}
                </span>
                <span className={`text-[10px] font-bold uppercase tracking-widest transition-colors duration-700 ${clr.label}`}>
                  {tipe}
                </span>
              </div>
              <div className="mt-1 text-xs text-zinc-500 truncate leading-tight">{corak}</div>
            </div>

            <div className="text-right shrink-0">
              <div className={`text-[22px] font-black tabular-nums leading-none transition-colors duration-700 ${clr.text}`}>
                {shiftAbsKeJamStr(est.estAbsMin)}
              </div>
              <div className={`text-xs tabular-nums mt-1 font-semibold transition-colors duration-700 ${remaining < 0 ? 'text-red-400' : 'text-zinc-600'}`}>
                {remStr}
              </div>
            </div>
          </div>

          {/* Progress bar */}
          <div className="mt-3.5 h-1 bg-zinc-800 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-colors duration-700 ${clr.bar}`}
              style={{ width: `${Math.round(progress * 100)}%`, transition: 'width 4s linear, background-color 0.7s ease' }}
            />
          </div>
        </div>
      </div>
    </div>
  )
}
