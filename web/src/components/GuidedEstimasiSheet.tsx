import { useEffect, useRef, useState } from "react";
import { estimasiFieldHint, previewEstimasi } from "../domain/estimasiUtils";
import type { MesinData, MesinTipe } from "../domain/types";
import { MachineSetupForm } from "./MachineSetupForm";
import { CheckIcon, ClockIcon, CloseIcon, MesinTipeIcon, ScheduleIcon } from "./Icons";

/** Terpandu (guided) ESTIMASI — satu field yang label/keyboard-nya menyesuaikan tipe mesin,
 * dengan pratinjau "≈ jam" hidup dari rumus murni yang sama dipakai commands.ts. Port 1:1 dari
 * GuidedEstimasiSheet.kt (aplikasi Android). Mesin tanpa corak (baru/belum diatur) mendapat
 * langkah setup singkat dengan MachineSetupForm (mendukung D408 koreksi & D405 speed). */
export function GuidedEstimasiSheet({
  mcNo,
  mesin,
  onDismiss,
  onSubmit,
  onQuickUpdate,
}: {
  mcNo: string;
  mesin: MesinData | null;
  onDismiss: () => void;
  onSubmit: (value: string) => void;
  onQuickUpdate: (corak: string, targetYard: number | null, tipe: MesinTipe, koreksi: number | null, speed: number | null) => void;
}) {
  const [activeMesin, setActiveMesin] = useState<MesinData | null>(mesin);
  const tipe: MesinTipe = activeMesin?.tipe ?? "TAPPET";
  const needsSetup = !activeMesin || activeMesin.corak.trim() === "" || activeMesin.corak.trim() === "-";
  const [needQuickCorakSetup, setNeedQuickCorakSetup] = useState(needsSetup);
  const [valueInput, setValueInput] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!needQuickCorakSetup) {
      const t = setTimeout(() => inputRef.current?.focus(), 100);
      return () => clearTimeout(t);
    }
  }, [needQuickCorakSetup]);

  function submit() {
    if (valueInput.trim() === "") return;
    onSubmit(`${mcNo} ${valueInput.trim()}`);
  }

  const hint = estimasiFieldHint(tipe);
  const preview = previewEstimasi(tipe, valueInput, activeMesin);

  return (
    <div className="dialog-backdrop" onClick={onDismiss}>
      <div className="dialog" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 420, maxHeight: "90vh", overflowY: "auto" }}>
        <div style={{ fontWeight: 800, fontSize: 16, marginBottom: 16, display: "flex", alignItems: "center", gap: 8 }}>
          <ScheduleIcon size={18} />
          <span>Update Estimasi — Mc {mcNo}</span>
          <span style={{ fontSize: 13, color: "var(--cyan-400)", display: "inline-flex", alignItems: "center", gap: 4 }}>
            <MesinTipeIcon tipe={tipe} size={13} />
            ({tipe})
          </span>
        </div>

        {needQuickCorakSetup ? (
          <MachineSetupForm
            initial={activeMesin ?? { tipe: "TAPPET", corak: "", targetYard: null, speed: null, koreksi: null }}
            onSave={(corak, targetYard, selectedTipe, koreksi, speed) => {
              const updated: MesinData = { tipe: selectedTipe, corak, targetYard, speed, koreksi };
              setActiveMesin(updated);
              onQuickUpdate(corak, targetYard, selectedTipe, koreksi, speed);
              setNeedQuickCorakSetup(false);
            }}
            onCancel={onDismiss}
          />
        ) : (
          <>
            <div className="field-label">{hint.label}</div>
            <input
              ref={inputRef}
              className="field-input"
              placeholder={`cth: ${hint.example}`}
              inputMode="decimal"
              value={valueInput}
              onChange={(e) => setValueInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") submit();
              }}
            />
            <div className="preview-hint" style={{ display: "flex", alignItems: "center", gap: 5 }}>
              <ClockIcon size={13} />
              <span>{preview ? `≈ jam ${preview}` : "Isi untuk melihat perkiraan jam"}</span>
            </div>
            <div className="actions" style={{ marginTop: 20 }}>
              <button className="cancel" onClick={onDismiss} style={{ display: "inline-flex", alignItems: "center", gap: 5 }}>
                <CloseIcon size={14} />
                <span>Batal</span>
              </button>
              <button
                className="confirm"
                style={{ background: "var(--cyan-600)", display: "inline-flex", alignItems: "center", gap: 5 }}
                disabled={valueInput.trim() === ""}
                onClick={submit}
              >
                <CheckIcon size={14} />
                <span>Simpan</span>
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}


