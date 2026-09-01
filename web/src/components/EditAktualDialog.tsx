import { useState } from "react";
import { useDoffStore } from "../store/DoffStore";
import { useUiStore } from "../store/UiStore";
import { formatYard, minOfDayToTimeStr } from "../domain/format";
import { parseJam, standarisasiKeterangan } from "../domain/parse";
import type { AktualEntry } from "../domain/types";
import { CheckIcon, ClockIcon, CloseIcon, DeleteIcon, EditIcon, RulerIcon, TagIcon, TextureIcon } from "./Icons";
import { CorakShortcutPicker, KeteranganShortcutPicker } from "./ShortcutPicker";

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
  onSaveCustom,
}: {
  entry: AktualEntry;
  onClose: () => void;
  onDelete: (id: number) => void;
  onSaveCustom?: (jamStr: string, newKet: string, corakOverride: string | null, customYard: number | null) => void;
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
    const corakVal = corak.trim() === "" ? null : corak.trim();
    if (onSaveCustom) {
      onSaveCustom(jamStr, newKet, corakVal, yardVal);
    } else {
      updateAktual(entry.id, jamStr, newKet, corakVal, yardVal);
    }
    onClose();
  }

  return (
    <div className="dialog-backdrop" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
          <div style={{ fontWeight: 800, fontSize: 16, display: "flex", alignItems: "center", gap: 8 }}>
            <EditIcon size={18} />
            <span>Edit Riwayat — Mc {entry.mcNo}</span>
          </div>
          <button
            className="icon-btn"
            style={{ color: "var(--red-500)" }}
            onClick={() => {
              onClose();
              onDelete(entry.id);
            }}
            aria-label="Hapus riwayat doffing"
            title="Hapus riwayat doffing"
          >
            <DeleteIcon />
          </button>
        </div>
        <div className="field-label" style={{ display: "flex", alignItems: "center", gap: 4 }}>
          <ClockIcon size={13} />
          <span>Jam</span>
        </div>
        <input
          className="field-input"
          placeholder="14.30"
          inputMode="numeric"
          value={jam}
          onChange={(e) => setJam(e.target.value)}
        />
        <div style={{ height: 12 }} />
        <div className="field-label" style={{ display: "flex", alignItems: "center", gap: 4 }}>
          <TagIcon size={13} />
          <span>Keterangan (opsional)</span>
        </div>
        <input className="field-input" value={extraKet} onChange={(e) => setExtraKet(e.target.value)} />
        <KeteranganShortcutPicker value={extraKet} onSelect={setExtraKet} />
        <div style={{ height: 12 }} />
        <div className="field-label" style={{ display: "flex", alignItems: "center", gap: 4 }}>
          <TextureIcon size={13} />
          <span>Corak (kosongkan untuk pakai default mesin)</span>
        </div>
        <input className="field-input" value={corak} onChange={(e) => setCorak(e.target.value.toUpperCase())} />
        <CorakShortcutPicker value={corak} onSelect={setCorak} />
        <div style={{ height: 12 }} />
        <div className="field-label" style={{ display: "flex", alignItems: "center", gap: 4 }}>
          <RulerIcon size={13} />
          <span>Yard Kustom</span>
        </div>
        <input className="field-input" placeholder="opsional" inputMode="decimal" value={yard} onChange={(e) => setYard(e.target.value)} />
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

