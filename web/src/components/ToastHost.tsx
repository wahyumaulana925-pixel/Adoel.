import { useEffect } from "react";
import { useUiStore } from "../store/UiStore";

export function ToastHost() {
  const { toast, dismissToast } = useUiStore();

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(dismissToast, 3500);
    return () => clearTimeout(t);
  }, [toast, dismissToast]);

  if (!toast) return null;

  return (
    <div className="toast-host">
      <div className="toast" key={toast.key}>
        <span style={{ flex: 1 }}>{toast.msg}</span>
        {toast.undo && (
          <button
            onClick={() => {
              toast.undo?.();
              dismissToast();
            }}
          >
            URUNGKAN
          </button>
        )}
      </div>
    </div>
  );
}
