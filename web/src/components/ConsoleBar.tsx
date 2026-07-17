import { useState, type ReactNode } from "react";
import { RedoIcon, ScheduleIcon, ScissorsIcon, UndoIcon } from "./Icons";

/** Bar konsol mengambang — satu field nomor mesin diapit Undo/Redo di kiri dan dua aksi
 * terpandu (Estimasi, Doffing) di kanan: `[Undo][Redo]  [nomor mesin]  [Estimasi][Doffing]`.
 * Tidak ada lagi input teks bebas atau tombol "Mulai" tunggal — operator memilih aksi lewat
 * ikon mana yang ditekan. Port 1:1 dari ConsoleBar.kt (aplikasi Android). */
export function ConsoleBar({
  onEstimasiClick,
  onDoffingClick,
  onUndo,
  onRedo,
  canUndo,
  canRedo,
}: {
  onEstimasiClick: (mcNo: string) => void;
  onDoffingClick: (mcNo: string) => void;
  onUndo: () => void;
  onRedo: () => void;
  canUndo: boolean;
  canRedo: boolean;
}) {
  const [mcNoInput, setMcNoInput] = useState("");

  function submit(action: (mcNo: string) => void) {
    if (mcNoInput.trim() !== "") {
      action(mcNoInput.trim());
      setMcNoInput("");
    }
  }

  return (
    <div className="console-bar floating-card">
      <div className="console-hint">Ketik nomor mesin, lalu ketuk jam (estimasi) atau gunting (doffing)</div>
      <div className="console-row">
        <ConsoleIconButton icon={<UndoIcon size={18} />} label="Undo" enabled={canUndo} accent="var(--violet-500)" onClick={onUndo} />
        <ConsoleIconButton icon={<RedoIcon size={18} />} label="Redo" enabled={canRedo} accent="var(--violet-500)" onClick={onRedo} />
        <input
          className="console-mcno-input"
          value={mcNoInput}
          onChange={(e) => setMcNoInput(e.target.value.replace(/\D/g, "").slice(0, 3))}
          onKeyDown={(e) => {
            if (e.key === "Enter") submit(onEstimasiClick);
          }}
          placeholder="Nomor mesin"
          inputMode="numeric"
          autoComplete="off"
        />
        <ConsoleIconButton
          icon={<ScheduleIcon size={20} />}
          label="Estimasi"
          enabled={mcNoInput.trim() !== ""}
          accent="var(--cyan-600)"
          onClick={() => submit(onEstimasiClick)}
        />
        <ConsoleIconButton
          icon={<ScissorsIcon size={20} />}
          label="Doffing"
          enabled={mcNoInput.trim() !== ""}
          accent="var(--emerald-500)"
          onClick={() => submit(onDoffingClick)}
        />
      </div>
    </div>
  );
}

function ConsoleIconButton({
  icon,
  label,
  enabled,
  accent,
  onClick,
}: {
  icon: ReactNode;
  label: string;
  enabled: boolean;
  accent: string;
  onClick: () => void;
}) {
  return (
    <button
      className="console-icon-btn"
      style={{ background: enabled ? accent : "var(--bg-elevated-2)", color: enabled ? "#fff" : "var(--text-faint)" }}
      disabled={!enabled}
      aria-label={label}
      onClick={onClick}
    >
      {icon}
    </button>
  );
}
