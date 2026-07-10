import { useUiStore } from "../store/UiStore";

export function ConfirmDialog() {
  const { confirm, dismissConfirm } = useUiStore();
  if (!confirm) return null;

  return (
    <div className="dialog-backdrop" onClick={dismissConfirm}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div className="msg">{confirm.msg}</div>
        <div className="actions">
          <button className="cancel" onClick={dismissConfirm}>
            Batal
          </button>
          <button
            className="confirm"
            onClick={() => {
              confirm.onConfirm();
              dismissConfirm();
            }}
          >
            Ya
          </button>
        </div>
      </div>
    </div>
  );
}
