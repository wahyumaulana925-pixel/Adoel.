import { useEffect } from 'react'
import { useUIStore } from '../store/useUIStore'

export function Toast() {
  const { toast, dismissToast } = useUIStore()

  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(dismissToast, 3000)
    return () => clearTimeout(timer)
  }, [toast?.key])

  if (!toast) return null

  return (
    <div className="fixed top-3 left-3 right-3 z-50 flex items-center gap-3 bg-zinc-800 border border-zinc-700 text-zinc-100 px-4 py-3 rounded-2xl shadow-2xl">
      <span className="flex-1 text-sm leading-tight">{toast.msg}</span>
      {toast.undo && (
        <button
          className="text-teal-400 font-bold text-sm shrink-0 px-1"
          onPointerDown={(e) => { e.stopPropagation(); toast.undo!(); dismissToast() }}
        >
          UNDO
        </button>
      )}
    </div>
  )
}
