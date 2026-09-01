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

  const hasInput = mcNoInput.trim() !== "";

  return (
    <div className="console-bar floating-card">
      <div className="console-row">
        <ConsoleIconButton
          icon={<UndoIcon size={18} />}
          label="Undo"
          title="Kembalikan aksi sebelumnya (Undo)"
          enabled={canUndo}
          accent="var(--amber-500)"
          onClick={onUndo}
        />
        <ConsoleIconButton
          icon={<RedoIcon size={18} />}
          label="Redo"
          title="Ulangi aksi (Redo)"
          enabled={canRedo}
          accent="var(--amber-500)"
          onClick={onRedo}
        />

        <input
          className={`console-mcno-input${hasInput ? " has-value" : ""}`}
          value={mcNoInput}
          onChange={(e) => setMcNoInput(e.target.value.replace(/\D/g, "").slice(0, 4))}
          onKeyDown={(e) => {
            if (e.key === "Enter") submit(onEstimasiClick);
          }}
          placeholder="No. Mc"
          inputMode="numeric"
          autoComplete="off"
          aria-label="Nomor mesin"
        />

        <ConsoleIconButton
          icon={<ScheduleIcon size={20} />}
          label="Estimasi"
          title="Buat estimasi waktu doffing baru"
          enabled={hasInput}
          accent="var(--cyan-600)"
          onClick={() => submit(onEstimasiClick)}
          pulse={hasInput}
        />
        <ConsoleIconButton
          icon={<ScissorsIcon size={20} />}
          label="Doffing"
          title="Catat doffing aktual sekarang"
          enabled={hasInput}
          accent="var(--emerald-500)"
          onClick={() => submit(onDoffingClick)}
          pulse={hasInput}
        />
      </div>
    </div>
  );
}

function ConsoleIconButton({
  icon,
  label,
  title,
  enabled,
  accent,
  pulse = false,
  onClick,
}: {
  icon: ReactNode;
  label: string;
  title?: string;
  enabled: boolean;
  accent: string;
  pulse?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      className={`console-icon-btn${enabled ? " enabled" : ""}${pulse ? " ready-pulse" : ""}`}
      style={{
        background: enabled ? accent : "var(--bg-elevated-2)",
        color: enabled ? "#fff" : "var(--text-faint)",
      }}
      disabled={!enabled}
      aria-label={label}
      title={title ?? label}
      onClick={onClick}
    >
      {icon}
    </button>
  );
}

