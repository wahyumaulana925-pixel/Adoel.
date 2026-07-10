import { useRef } from "react";

const KETERANGAN_CHIPS = ["HB", "P.LP", "P.SN", "P.OH", "P.EL", "P.Sel"];

export type Mode = "ESTIMASI" | "AKTUAL";

interface Props {
  mode: Mode;
  onModeChange: (mode: Mode) => void;
  value: string;
  onChange: (value: string) => void;
  onSend: () => void;
  hint: string | null;
  errorFlash: boolean;
  sendOk: boolean;
}

export function ConsoleBar({ mode, onModeChange, value, onChange, onSend, hint, errorFlash, sendOk }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);

  return (
    <div className="console-bar">
      {mode === "AKTUAL" && (
        <div className="chip-row">
          {KETERANGAN_CHIPS.map((code) => (
            <button
              key={code}
              className="chip"
              onClick={() => {
                onChange((value.trimEnd() + " " + code).trimStart());
                inputRef.current?.focus();
              }}
            >
              {code}
            </button>
          ))}
        </div>
      )}

      <div className="mode-toggle">
        <button
          className={mode === "ESTIMASI" ? "active-estimasi" : ""}
          onClick={() => onModeChange("ESTIMASI")}
        >
          ESTIMASI
        </button>
        <button className={mode === "AKTUAL" ? "active-doffing" : ""} onClick={() => onModeChange("AKTUAL")}>
          DOFFING
        </button>
      </div>

      <div className="command-row">
        <input
          ref={inputRef}
          value={value}
          onChange={(e) => onChange(e.target.value.toUpperCase())}
          onKeyDown={(e) => {
            if (e.key === "Enter") onSend();
          }}
          placeholder={mode === "ESTIMASI" ? "cth: 31 45" : "cth: 31 HB"}
          className={errorFlash ? "error-flash" : ""}
          autoCapitalize="characters"
          autoComplete="off"
          autoCorrect="off"
          spellCheck={false}
          inputMode="text"
        />
        <button className={`send${sendOk ? " ok" : ""}`} onClick={onSend} aria-label="Kirim">
          {sendOk ? <CheckIcon /> : <SendIcon />}
        </button>
      </div>
      {hint && <div className="input-hint">{hint}</div>}
    </div>
  );
}

function SendIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
      <path d="M22 2 11 13" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M22 2 15 22l-4-9-9-4 20-7Z" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
      <path d="M20 6 9 17l-5-5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
