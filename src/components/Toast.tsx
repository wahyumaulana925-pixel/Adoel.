import { useEffect } from 'react'
import { useUIStore } from '../store/useUIStore'
import { useKeyboard } from '../hooks/useKeyboard'

export function Toast() {
  const { toast, dismissToast } = useUIStore()
  const kbH = useKeyboard()

  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(dismissToast, 3000)
    return () => clearTimeout(timer)
  }, [toast?.key])

  if (!toast) return null

  // When keyboard is visible, show toast just above it instead of top of screen
  const aboveKeyboard = kbH > 80
  const posStyle = aboveKeyboard
    ? { bottom: kbH + 8, top: 'auto' as const }
    : { top: 12, bottom: 'auto' as const }

  return (
    <div
      key={toast.key}
      className={`fixed left-3 right-3 z-50 ${aboveKeyboard ? 'animate-toast-up' : 'animate-toast-in'}`}
      style={posStyle}
    >
      <div className="flex items-center gap-3 bg-zinc-800 border border-zinc-700/80 text-zinc-100 px-4 py-3.5 rounded-2xl shadow-2xl shadow-black/40">
        <span className="flex-1 text-sm leading-snug">{toast.msg}</span>
        {toast.undo && (
          <button
            className="text-cyan-400 font-bold text-xs tracking-wide shrink-0 px-2 py-1 rounded-lg active:bg-cyan-900/40 transition-colors duration-150"
            onPointerDown={(e) => { e.stopPropagation(); toast.undo!(); dismissToast() }}
          >
            UNDO
          </button>
        )}
      </div>
    </div>
  )
}
