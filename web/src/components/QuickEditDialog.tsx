import { useState } from "react";
import { useDoffStore } from "../store/DoffStore";
import { useUiStore } from "../store/UiStore";
import { formatYard } from "../domain/format";

/** Jalur cepat untuk 2 field yang paling sering berubah di lantai produksi — corak
 * & target yard — dijangkau lewat tap kartu radar, tanpa perlu buka Pengaturan >
 * Mesin. Mengubah data mesin PERMANEN (sama seperti versi Android), bukan cuma
 * override sekali pakai. */
export function QuickEditDialog({ mcNo, onClose }: { mcNo: string; onClose: () => void }) {
  const { state, setMesin } = useDoffStore();
  const { showToast } = useUiStore();
  const mesin = state.db[mcNo];
  const [corak, setCorak] = useState(mesin?.corak === "-" ? "" : mesin?.corak ?? "");
  const [targetYard, setTargetYard] = useState(mesin?.targetYard != null ? formatYard(mesin.targetYard) : "");

  if (!mesin) return null;

  function handleSave() {
    const trimmed = corak.trim() || "-";
    let yard: number | null;
    if (targetYard.trim() === "") {
      yard = null;
    } else {
      const parsed = parseFloat(targetYard.trim().replace(",", "."));
      yard = Number.isNaN(parsed) ? mesin.targetYard : parsed;
    }
    setMesin(mcNo, { ...mesin, corak: trimmed, targetYard: yard });
    showToast(`Mc ${mcNo} disimpan ✓`);
    onClose();
  }

  return (
    <div className="dialog-backdrop" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div style={{ fontWeight: 800, fontSize: 16, marginBottom: 16 }}>Ganti Cepat — Mc {mcNo}</div>
        <div className="field-label">Corak</div>
        <input className="field-input" value={corak} onChange={(e) => setCorak(e.target.value)} />
        <div style={{ height: 12 }} />
        <div className="field-label">Target Yard</div>
        <input
          className="field-input"
          placeholder="opsional"
          inputMode="decimal"
          value={targetYard}
          onChange={(e) => setTargetYard(e.target.value)}
        />
        <div className="actions" style={{ marginTop: 18 }}>
          <button className="cancel" onClick={onClose}>
            Batal
          </button>
          <button className="confirm" style={{ background: "var(--cyan-600)" }} onClick={handleSave}>
            Simpan
          </button>
        </div>
      </div>
    </div>
  );
}
