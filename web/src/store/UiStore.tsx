import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from "react";

interface ToastState {
  key: number;
  msg: string;
  undo?: () => void;
}

interface ConfirmState {
  msg: string;
  onConfirm: () => void;
}

interface UiStore {
  toast: ToastState | null;
  showToast: (msg: string, undo?: () => void) => void;
  dismissToast: () => void;
  confirm: ConfirmState | null;
  showConfirm: (msg: string, onConfirm: () => void) => void;
  dismissConfirm: () => void;
}

const Ctx = createContext<UiStore | null>(null);

export function UiStoreProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<ToastState | null>(null);
  const [confirm, setConfirm] = useState<ConfirmState | null>(null);
  const keyRef = useRef(0);

  const showToast = useCallback((msg: string, undo?: () => void) => {
    keyRef.current += 1;
    setToast({ key: keyRef.current, msg, undo });
  }, []);

  const dismissToast = useCallback(() => setToast(null), []);

  const showConfirm = useCallback((msg: string, onConfirm: () => void) => {
    setConfirm({ msg, onConfirm });
  }, []);

  const dismissConfirm = useCallback(() => setConfirm(null), []);

  return (
    <Ctx.Provider value={{ toast, showToast, dismissToast, confirm, showConfirm, dismissConfirm }}>
      {children}
    </Ctx.Provider>
  );
}

export function useUiStore(): UiStore {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useUiStore must be used within UiStoreProvider");
  return ctx;
}
