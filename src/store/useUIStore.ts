import { create } from 'zustand'

interface Toast {
  key: number
  msg: string
  undo?: () => void
}

interface Confirm {
  msg: string
  onConfirm: () => void
  onCancel?: () => void
}

interface UIStore {
  toast: Toast | null
  confirm: Confirm | null
  _toastKey: number
  showToast: (msg: string, undo?: () => void) => void
  dismissToast: () => void
  showConfirm: (msg: string, onConfirm: () => void, onCancel?: () => void) => void
  dismissConfirm: () => void
}

export const useUIStore = create<UIStore>((set, get) => ({
  toast: null,
  confirm: null,
  _toastKey: 0,

  showToast: (msg, undo) => {
    const key = get()._toastKey + 1
    set({ toast: { key, msg, undo }, _toastKey: key })
  },

  dismissToast: () => set({ toast: null }),

  showConfirm: (msg, onConfirm, onCancel) => set({ confirm: { msg, onConfirm, onCancel } }),

  dismissConfirm: () => set({ confirm: null }),
}))
