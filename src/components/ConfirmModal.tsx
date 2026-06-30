import { useUIStore } from '../store/useUIStore'

export function ConfirmModal() {
  const { confirm, dismissConfirm } = useUIStore()

  if (!confirm) return null

  return (
    <div
      className="fixed inset-0 bg-black/70 z-50 flex items-end sm:items-center justify-center"
      onPointerDown={dismissConfirm}
    >
      <div
        className="w-full sm:max-w-sm bg-zinc-900 border border-zinc-700 rounded-t-3xl sm:rounded-3xl p-6 mx-0 sm:mx-4"
        onPointerDown={(e) => e.stopPropagation()}
      >
        <p className="text-zinc-100 text-sm leading-relaxed mb-6">{confirm.msg}</p>
        <div className="flex gap-3">
          <button
            className="flex-1 py-3 rounded-2xl border border-zinc-600 text-zinc-400 text-sm font-medium active:bg-zinc-800"
            onClick={() => { confirm.onCancel?.(); dismissConfirm() }}
          >
            Batal
          </button>
          <button
            className="flex-1 py-3 rounded-2xl bg-red-600 text-white text-sm font-semibold active:bg-red-700"
            onClick={() => { confirm.onConfirm(); dismissConfirm() }}
          >
            Hapus
          </button>
        </div>
      </div>
    </div>
  )
}
