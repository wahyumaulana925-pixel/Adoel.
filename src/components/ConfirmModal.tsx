import { useUIStore } from '../store/useUIStore'

export function ConfirmModal() {
  const { confirm, dismissConfirm } = useUIStore()

  if (!confirm) return null

  return (
    <div
      className="fixed inset-0 bg-black/70 z-50 flex items-end sm:items-center justify-center animate-fade-in"
      onPointerDown={dismissConfirm}
    >
      <div
        className="w-full sm:max-w-sm bg-zinc-900 border border-zinc-800 rounded-t-3xl sm:rounded-3xl px-5 pt-6 pb-safe-floor sm:pb-6 animate-slide-up"
        onPointerDown={(e) => e.stopPropagation()}
      >
        {/* Handle */}
        <div className="w-10 h-1 bg-zinc-700 rounded-full mx-auto mb-5 sm:hidden" />

        <p className="text-zinc-200 text-[15px] leading-relaxed mb-6">{confirm.msg}</p>

        <div className="flex gap-3">
          <button
            className="flex-1 py-3 rounded-2xl border border-zinc-700 text-zinc-400 text-sm font-medium active:bg-zinc-800 transition-colors duration-150"
            onClick={() => { confirm.onCancel?.(); dismissConfirm() }}
          >
            Batal
          </button>
          <button
            className="flex-1 py-3 rounded-2xl bg-red-600/90 text-white text-sm font-semibold active:bg-red-600 transition-colors duration-150"
            onClick={() => { confirm.onConfirm(); dismissConfirm() }}
          >
            Hapus
          </button>
        </div>
      </div>
    </div>
  )
}
