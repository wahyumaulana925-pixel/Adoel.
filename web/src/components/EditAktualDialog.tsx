import { useState } from "react";
import { useDoffStore } from "../store/DoffStore";
import { useUiStore } from "../store/UiStore";
import { formatYard } from "../domain/format";
import type { AktualEntry } from "../domain/types";
import { DeleteIcon } from "./Icons";

export function EditAktualDialog({
  entry,
  onClose,
  onDelete,
}: {
  entry: AktualEntry;
  onClose: () => void;
  onDelete: (id: number) => void;
}) {
  const { updateAktual } = useDoffStore();
  const { showToast } = useUiStore();
  const [ket, setKet] = useState(entry.ket);
  const [corak, setCorak] = useState(entry.corakOverride ?? "");
  const [yard, setYard] = useState(entry.customYard != null ? formatYard(entry.customYard) : "");

  function handleSave() {
    if (ket.trim() === "") {
      showToast("Keterangan tidak boleh kosong");
      return;
    }
    let yardVal: number | null = null;
    if (yard.trim() !== "") {
      const parsed = parseFloat(yard.trim().replace(",", "."));
      if (Number.isNaN(parsed)) {
        showToast("Yard tidak valid");
        return;
      }
      yardVal = parsed;
    }
    updateAktual(entry.id, ket.trim(), corak.trim() === "" ? null : corak.trim(), yardVal);
    onClose();
  }

  return (
    <div className="dialog-backdrop" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
          <div style={{ fontWeight: 800, fontSize: 16 }}>Edit Riwayat — Mc {entry.mcNo}</div>
          <button
            className="icon-btn"
            style={{ color: "var(--red-500)" }}
            onClick={() => {
              onClose();
              onDelete(entry.id);
            }}
            aria-label="Hapus"
          >
            <DeleteIcon />
          </button>
        </div>
        <div className="field-label">Keterangan</div>
        <input className="field-input" value={ket} onChange={(e) => setKet(e.target.value)} />
        <div style={{ height: 12 }} />
        <div className="field-label">Corak (kosongkan untuk pakai default mesin)</div>
        <input className="field-input" value={corak} onChange={(e) => setCorak(e.target.value)} />
        <div style={{ height: 12 }} />
        <div className="field-label">Yard Kustom</div>
        <input className="field-input" placeholder="opsional" inputMode="decimal" value={yard} onChange={(e) => setYard(e.target.value)} />
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
