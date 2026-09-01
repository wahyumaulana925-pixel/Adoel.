import { useState } from "react";
import { formatYard, nowTimeStr } from "../domain/format";
import { parseJam } from "../domain/parse";
import { selisihKoreksiD408 } from "../domain/estimasiUtils";
import { useUiStore } from "../store/UiStore";
import type { MesinData, MesinTipe } from "../domain/types";
import { CorakShortcutPicker } from "./CorakShortcutPicker";

const TIPE_LIST: MesinTipe[] = ["TAPPET", "CAM", "D405", "D408"];

function parseNum(raw: string): number | null {
  const t = raw.trim().replace(",", ".");
  if (t === "") return null;
  const n = parseFloat(t);
  return Number.isNaN(n) ? null : n;
}

export function MachineSetupForm({
  initial,
  onSave,
  onCancel,
}: {
  initial: MesinData;
  onSave: (corak: string, targetYard: number | null, tipe: MesinTipe, koreksi: number | null, speed: number | null) => void;
  onCancel: () => void;
}) {
  const { showToast } = useUiStore();
  const [tipe, setTipe] = useState<MesinTipe>(initial.tipe);
  const [corak, setCorak] = useState(initial.corak !== "-" ? initial.corak : "");
  const [targetYardText, setTargetYardText] = useState(initial.targetYard != null ? formatYard(initial.targetYard) : "");
  const [speedText, setSpeedText] = useState(initial.speed != null ? formatYard(initial.speed) : "");
  const [koreksiText, setKoreksiText] = useState(initial.koreksi != null ? formatYard(initial.koreksi) : "");

  // Helper selisih hitung koreksi D408
  const [waktuAktualText, setWaktuAktualText] = useState(() => nowTimeStr());
  const [counterText, setCounterText] = useState("");

  const handleAdjustKoreksi = (delta: number) => {
    const current = parseNum(koreksiText) ?? 0;
    const nextVal = current + delta;
    setKoreksiText(String(nextVal));
  };

  const handleApplyKoreksiHelper = () => {
    const wakMin = parseJam(waktuAktualText);
    const cntMin = parseJam(counterText);
    if (wakMin == null || cntMin == null) {
      showToast("⚠ Format jam aktual atau counter tidak valid (cth: 14.30)");
      return;
    }
    const diff = selisihKoreksiD408(wakMin, cntMin);
    const formatted = String(diff);
    setKoreksiText(formatted);
    showToast(`Koreksi diatur ke ${diff > 0 ? `+${diff}` : diff} menit ✓`);
  };

  const handleValidateAndSave = () => {
    const corakTrim = corak.trim();
    if (!corakTrim) {
      showToast("⚠ Corak wajib diisi");
      return;
    }

    if (targetYardText.trim() !== "" && parseNum(targetYardText) === null) {
      showToast("⚠ Target Yard tidak valid, cek kembali");
      return;
    }

    if (tipe === "D405" && speedText.trim() !== "" && parseNum(speedText) === null) {
      showToast("⚠ Speed tidak valid, cek kembali");
      return;
    }

    if (tipe === "D408" && koreksiText.trim() !== "" && parseNum(koreksiText) === null) {
      showToast("⚠ Koreksi tidak valid, cek kembali");
      return;
    }

    const yard = parseNum(targetYardText);
    const speed = tipe === "D405" ? parseNum(speedText) : null;
    const koreksi = tipe === "D408" ? parseNum(koreksiText) : null;

    onSave(corakTrim, yard, tipe, koreksi, speed);
  };

  return (
    <div>
      <div className="field-label">Tipe Mesin</div>
      <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 16 }}>
        {TIPE_LIST.map((t) => (
          <button
            key={t}
            type="button"
            className={`chip-btn${tipe === t ? " active" : ""}`}
            onClick={() => {
              setTipe(t);
              if (t !== "D405") setSpeedText("");
              if (t !== "D408") setKoreksiText("");
            }}
          >
            {t}
          </button>
        ))}
      </div>

      <div className="field-label">Corak</div>
      <input
        className="field-input"
        placeholder="contoh: 4500"
        value={corak}
        onChange={(e) => setCorak(e.target.value.toUpperCase())}
      />
      <CorakShortcutPicker value={corak} onSelect={setCorak} />

      <div style={{ height: 12 }} />
      <div className="field-label">Target Yard (opsional)</div>
      <input
        className="field-input"
        inputMode="decimal"
        placeholder="contoh: 300"
        value={targetYardText}
        onChange={(e) => setTargetYardText(e.target.value)}
      />

      {tipe === "D405" && (
        <div style={{ marginTop: 12 }}>
          <div className="field-label">Speed Mesin (yard/menit)</div>
          <input
            className="field-input"
            inputMode="decimal"
            placeholder="contoh: 0.158"
            value={speedText}
            onChange={(e) => setSpeedText(e.target.value)}
          />
          <div style={{ fontSize: 11, color: "var(--text-faint)", marginTop: 4 }}>
            Dipakai untuk menghitung estimasi doff dari sisa yard.
          </div>
        </div>
      )}

      {tipe === "D408" && (
        <div style={{ marginTop: 12 }}>
          <div className="field-label">Koreksi Counter (menit)</div>
          <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <button
              type="button"
              className="chip-btn"
              style={{ width: 44, height: 44, fontSize: 18, fontWeight: 700 }}
              onClick={() => handleAdjustKoreksi(-1)}
            >
              −
            </button>
            <input
              className="field-input"
              inputMode="decimal"
              placeholder="contoh: 0 atau -15"
              value={koreksiText}
              onChange={(e) => setKoreksiText(e.target.value)}
              style={{ flex: 1, textAlign: "center" }}
            />
            <button
              type="button"
              className="chip-btn"
              style={{ width: 44, height: 44, fontSize: 18, fontWeight: 700 }}
              onClick={() => handleAdjustKoreksi(1)}
            >
              +
            </button>
          </div>

          {/* Helper hitung selisih koreksi persis seperti di pengaturan database */}
          <div style={{ background: "var(--bg-elevated-2)", padding: 12, borderRadius: 10, marginTop: 12 }}>
            <div style={{ fontSize: 12, fontWeight: 700, color: "var(--cyan-400)", marginBottom: 8 }}>
              Hitung Koreksi dari Jam
            </div>
            <div className="field-grid" style={{ marginBottom: 8 }}>
              <div>
                <div style={{ fontSize: 10, color: "var(--text-faint)", marginBottom: 2 }}>Waktu Nyata</div>
                <input
                  className="field-input"
                  value={waktuAktualText}
                  onChange={(e) => setWaktuAktualText(e.target.value)}
                  placeholder="14.00"
                  style={{ fontSize: 12, padding: "6px 8px" }}
                />
              </div>
              <div>
                <div style={{ fontSize: 10, color: "var(--text-faint)", marginBottom: 2 }}>Jam di Counter</div>
                <input
                  className="field-input"
                  value={counterText}
                  onChange={(e) => setCounterText(e.target.value)}
                  placeholder="13.45"
                  style={{ fontSize: 12, padding: "6px 8px" }}
                />
              </div>
            </div>
            <button
              type="button"
              className="confirm"
              style={{ width: "100%", padding: "8px 12px", fontSize: 12, background: "var(--cyan-600)" }}
              onClick={handleApplyKoreksiHelper}
            >
              Hitung & Terapkan Koreksi
            </button>
          </div>
        </div>
      )}

      <div className="actions" style={{ marginTop: 20 }}>
        <button type="button" className="cancel" onClick={onCancel}>
          Batal
        </button>
        <button
          type="button"
          className="confirm"
          style={{ background: "var(--cyan-600)" }}
          onClick={handleValidateAndSave}
        >
          Simpan &amp; Lanjut
        </button>
      </div>
    </div>
  );
}
