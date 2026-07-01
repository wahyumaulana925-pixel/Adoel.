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

const REVEAL_W = 180

function urgency(remaining: number) {
  if (remaining > 30) return { accent: '#06b6d4', bar: 'bg-cyan-500',   text: 'text-cyan-400',   label: 'text-cyan-700',   pulse: false }
  if (remaining > 10) return { accent: '#f59e0b', bar: 'bg-amber-400',  text: 'text-amber-400',  label: 'text-amber-700',  pulse: false }
  if (remaining > 0)  return { accent: '#f97316', bar: 'bg-orange-500', text: 'text-orange-400', label: 'text-orange-700', pulse: false }
  return               { accent: '#ef4444', bar: 'bg-red-500',    text: 'text-red-400',    label: 'text-red-700',    pulse: true  }
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
      <polyline points="20 6 9 17 4 12" />
    </svg>
  )
}

function TrashIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-4 h-4">
      <polyline points="3 6 5 6 21 6" />
      <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
      <path d="M10 11v6M14 11v6" />
      <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
    </svg>
  )
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
  const showDot   = remaining <= 5

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
    <div className="relative rounded-2xl overflow-hidden" style={{ height: 80 }}>
      {/* Action strip — revealed on left-swipe; HAPUS left, DOFF right */}
      <div className="absolute inset-y-0 right-0 flex items-center justify-end pr-3 gap-2" style={{ width: REVEAL_W }}>
        <button
          className="flex flex-col items-center justify-center gap-1 bg-zinc-700 active:bg-zinc-600 text-zinc-200 rounded-xl"
          style={{ width: 72, height: '80%' }}
          onClick={() => { snap(false); onHapus() }}
        >
          <TrashIcon />
          <span className="text-[10px] font-bold tracking-wide">HAPUS</span>
        </button>
        <button
          className="flex flex-col items-center justify-center gap-1 bg-cyan-600 active:bg-cyan-500 text-white rounded-xl"
          style={{ width: 88, height: '80%' }}
          onClick={() => { snap(false); onDoff() }}
        >
          <CheckIcon />
          <span className="text-[10px] font-bold tracking-wide">DOFF</span>
        </button>
      </div>

      {/* Card face */}
      <div
        ref={innerRef}
        className={`relative h-full bg-zinc-900 rounded-2xl select-none overflow-hidden ${clr.pulse ? 'animate-pulse-soft' : ''}`}
        style={{
          borderLeft: `3px solid ${clr.accent}`,
          boxShadow: `inset 0 0 0 1px ${clr.accent}18`,
        }}
        onTouchStart={onTouchStart}
        onTouchMove={onTouchMove}
        onTouchEnd={onTouchEnd}
        onMouseDown={onMouseDown}
      >
        {/* Progress fill — grows from left as subtle tinted background */}
        <div
          className={`absolute left-0 top-0 bottom-0 ${clr.bar} opacity-[0.12]`}
          style={{ width: `${Math.round(progress * 100)}%`, transition: 'width 4s linear' }}
        />

        {/* Content */}
        <div className="relative z-10 h-full flex items-center px-4 gap-3">
          {/* Left: machine number + tipe + corak */}
          <div className="min-w-0 flex-1">
            <div className="flex items-baseline gap-2">
              <span className="text-4xl font-black tracking-tighter text-zinc-100 tabular-nums leading-none">
                {est.mcNo}
              </span>
              <span className={`text-[10px] font-bold uppercase tracking-widest ${clr.label}`}>
                {tipe}
              </span>
            </div>
            <div className="text-[11px] font-bold uppercase tracking-wide text-zinc-500 truncate mt-0.5">
              {corak}
            </div>
          </div>

          {/* Right: ping dot + est time + remaining */}
          <div className="flex items-center gap-2.5 shrink-0">
            {showDot && (
              <span className="relative flex h-3 w-3 shrink-0">
                <span className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${remaining < 0 ? 'bg-red-400' : 'bg-emerald-400'}`} />
                <span className={`relative inline-flex rounded-full h-3 w-3 ${remaining < 0 ? 'bg-red-500' : 'bg-emerald-500'}`} />
              </span>
            )}
            <div className="text-right">
              <div className={`text-3xl font-bold tracking-tighter tabular-nums leading-none font-mono ${clr.text}`}>
                {shiftAbsKeJamStr(est.estAbsMin)}
              </div>
              <div className={`text-xs font-black uppercase tracking-wider tabular-nums mt-0.5 ${remaining < 0 ? 'text-red-400' : clr.text}`}>
                {remStr}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
