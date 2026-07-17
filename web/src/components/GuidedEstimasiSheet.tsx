import { useEffect, useRef, useState } from "react";
import { estimasiFieldHint, previewEstimasi } from "../domain/estimasiUtils";
import type { MesinData, MesinTipe } from "../domain/types";

/** Terpandu (guided) ESTIMASI — satu field yang label/keyboard-nya menyesuaikan tipe mesin,
 * dengan pratinjau "≈ jam" hidup dari rumus murni yang sama dipakai commands.ts. Port 1:1 dari
 * GuidedEstimasiSheet.kt (aplikasi Android). Mesin tanpa corak (baru/belum diatur) mendapat
 * langkah setup corak+target yard singkat dulu, bukan ditolak keluar ke Pengaturan. */
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
  onQuickUpdate: (corak: string, targetYard: number | null) => void;
}) {
  const tipe: MesinTipe = mesin?.tipe ?? "TAPPET";
  const needsSetup = !mesin || mesin.corak.trim() === "" || mesin.corak.trim() === "-";
  const [needQuickCorakSetup, setNeedQuickCorakSetup] = useState(needsSetup);
  const [corakInput, setCorakInput] = useState("");
  const [targetYardInput, setTargetYardInput] = useState("");
  const [valueInput, setValueInput] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const t = setTimeout(() => inputRef.current?.focus(), 100);
    return () => clearTimeout(t);
  }, [needQuickCorakSetup]);

  function submit() {
    if (valueInput.trim() === "") return;
    onSubmit(`${mcNo} ${valueInput.trim()}`);
  }

  function saveSetup() {
    if (corakInput.trim() === "") return;
    const yard = targetYardInput.trim() === "" ? null : parseFloat(targetYardInput.trim().replace(",", "."));
    onQuickUpdate(corakInput.trim(), yard != null && !Number.isNaN(yard) ? yard : null);
    setNeedQuickCorakSetup(false);
  }

  const hint = estimasiFieldHint(tipe);
  const preview = previewEstimasi(tipe, valueInput, mesin);

  return (
    <div className="dialog-backdrop" onClick={onDismiss}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div style={{ fontWeight: 800, fontSize: 16, marginBottom: 16 }}>
          Update Estimasi — Mc {mcNo} ({tipe})
        </div>

        {needQuickCorakSetup ? (
          <>
            <div className="field-label">Masukkan Corak Baru</div>
            <input
              ref={inputRef}
              className="field-input"
              placeholder="cth: 34758"
              inputMode="numeric"
              value={corakInput}
              onChange={(e) => setCorakInput(e.target.value.toUpperCase())}
            />
            <div style={{ height: 12 }} />
            <div className="field-label">Target Yard (Opsional)</div>
            <input
              className="field-input"
              placeholder="cth: 303"
              inputMode="decimal"
              value={targetYardInput}
              onChange={(e) => setTargetYardInput(e.target.value)}
            />
            <button
              className="btn primary full"
              style={{ marginTop: 20 }}
              disabled={corakInput.trim() === ""}
              onClick={saveSetup}
            >
              Simpan Corak &amp; Lanjut
            </button>
          </>
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
            <div className="preview-hint">{preview ? `≈ jam ${preview}` : "Isi untuk melihat perkiraan jam"}</div>
            <div className="actions" style={{ marginTop: 20 }}>
              <button className="cancel" onClick={onDismiss}>
                Batal
              </button>
              <button
                className="confirm"
                style={{ background: "var(--cyan-600)" }}
                disabled={valueInput.trim() === ""}
                onClick={submit}
              >
                Simpan
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
