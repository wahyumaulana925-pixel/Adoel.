import { useUIStore } from '../store/useUIStore'

export function ConfirmModal() {
  const { confirm, dismissConfirm } = useUIStore()

  if (!confirm) return null

  return (
    <div
      className="fixed inset-0 bg-black/80 z-50 flex items-center justify-center px-5 animate-fade-in"
      onPointerDown={dismissConfirm}
    >
      <div
        className="w-full max-w-sm bg-zinc-800 border border-zinc-700 rounded-3xl px-5 pt-6 pb-6 shadow-2xl shadow-black/60 animate-scale-in"
        onPointerDown={(e) => e.stopPropagation()}
      >
        <p className="text-zinc-200 text-[15px] leading-relaxed mb-6">{confirm.msg}</p>

        <div className="flex gap-3">
          <button
            className="flex-1 py-3 rounded-2xl border border-zinc-600 text-zinc-400 text-sm font-medium active:bg-zinc-700 transition-colors duration-150"
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
