import { useState } from "react";
import { useDoffStore } from "../store/DoffStore";
import { useUiStore } from "../store/UiStore";
import { formatYard } from "../domain/format";
import { CheckIcon, CloseIcon, EditIcon, RulerIcon, TextureIcon } from "./Icons";
import { CorakShortcutPicker } from "./CorakShortcutPicker";

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
        <div style={{ fontWeight: 800, fontSize: 16, marginBottom: 16, display: "flex", alignItems: "center", gap: 8 }}>
          <EditIcon size={18} />
          <span>Ganti Cepat — Mc {mcNo}</span>
        </div>
        <div className="field-label" style={{ display: "flex", alignItems: "center", gap: 4 }}>
          <TextureIcon size={13} />
          <span>Corak</span>
        </div>
        <input className="field-input" value={corak} onChange={(e) => setCorak(e.target.value.toUpperCase())} />
        <CorakShortcutPicker value={corak} onSelect={setCorak} />
        <div style={{ height: 12 }} />
        <div className="field-label" style={{ display: "flex", alignItems: "center", gap: 4 }}>
          <RulerIcon size={13} />
          <span>Target Yard</span>
        </div>
        <input
          className="field-input"
          placeholder="opsional"
          inputMode="decimal"
          value={targetYard}
          onChange={(e) => setTargetYard(e.target.value)}
        />
        <div className="actions" style={{ marginTop: 18 }}>
          <button className="cancel" onClick={onClose} style={{ display: "inline-flex", alignItems: "center", gap: 5 }}>
            <CloseIcon size={14} />
            <span>Batal</span>
          </button>
          <button
            className="confirm"
            style={{ background: "var(--cyan-600)", display: "inline-flex", alignItems: "center", gap: 5 }}
            onClick={handleSave}
          >
            <CheckIcon size={14} />
            <span>Simpan</span>
          </button>
        </div>
      </div>
    </div>
  );
}

