import { useState } from "react";
import { useDoffStore } from "../store/DoffStore";
import { useUiStore } from "../store/UiStore";
import { formatYard, minOfDayToTimeStr } from "../domain/format";
import { parseJam, standarisasiKeterangan } from "../domain/parse";
import type { AktualEntry } from "../domain/types";
import { DeleteIcon } from "./Icons";

/** ket tersimpan sebagai "jam(extra)" atau cuma "jam" kalau tanpa keterangan tambahan
 * (lihat prosesBarisUmum di commands.ts) — field Jam & Keterangan di dialog ini dulu
 * digabung jadi satu teks bebas berbasis ket, jadi mengedit jam berarti retype semuanya
 * termasuk tanda kurungnya. Dipisah di sini supaya tiap field independen: jam.mcNo yang
 * sudah benar tidak perlu diketik ulang hanya karena mau mengoreksi jam, dan sebaliknya. */
function extractExtraKeterangan(ket: string, jam: string): string {
  if (!ket.startsWith(jam)) return "";
  const rest = ket.slice(jam.length);
  const m = /^\(([^)]*)\)$/.exec(rest);
  return m ? m[1] : "";
}

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
  const [jam, setJam] = useState(entry.jam);
  const [extraKet, setExtraKet] = useState(extractExtraKeterangan(entry.ket, entry.jam));
  const [corak, setCorak] = useState(entry.corakOverride ?? "");
  const [yard, setYard] = useState(entry.customYard != null ? formatYard(entry.customYard) : "");

  function handleSave() {
    const jamMin = parseJam(jam.trim());
    if (jamMin === null) {
      showToast("Jam tidak valid — format 14.30");
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
    const jamStr = minOfDayToTimeStr(jamMin);
    const extra = standarisasiKeterangan(extraKet.trim());
    const newKet = extra.length > 0 ? `${jamStr}(${extra})` : jamStr;
    updateAktual(entry.id, jamStr, newKet, corak.trim() === "" ? null : corak.trim(), yardVal);
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
        <div className="field-label">Jam</div>
        <input
          className="field-input"
          placeholder="14.30"
          inputMode="numeric"
          value={jam}
          onChange={(e) => setJam(e.target.value)}
        />
        <div style={{ height: 12 }} />
        <div className="field-label">Keterangan (opsional)</div>
        <input className="field-input" value={extraKet} onChange={(e) => setExtraKet(e.target.value)} />
        <div style={{ height: 12 }} />
        <div className="field-label">Corak (kosongkan untuk pakai default mesin)</div>
        <input className="field-input" inputMode="numeric" value={corak} onChange={(e) => setCorak(e.target.value)} />
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
