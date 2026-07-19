import { useMemo, useRef, useState } from "react";
import { useDoffStore } from "../store/DoffStore";
import { useUiStore } from "../store/UiStore";
import { TIPE_COLOR } from "../domain/mesinVisual";
import { formatYard } from "../domain/format";
import type { MesinData, MesinTipe, ThemeMode } from "../domain/types";
import { CloseIcon, EditIcon, MesinTipeIcon } from "./Icons";
import { WovenDivider } from "./WovenDivider";

const TIPE_LIST: MesinTipe[] = ["TAPPET", "CAM", "D405", "D408"];

/** Parse angka field mesin sama seperti `it.replace(',', '.').toDoubleOrNull()` di Android:
 * menerima koma sebagai titik desimal, dan MEMPERTAHANKAN 0 (koreksi D408 0 itu valid — tanpa
 * koreksi). Sengaja BUKAN `parseFloat(x) || null`, yang salah mengubah 0 menjadi null. */
function parseMesinNum(raw: string): number | null {
  const t = raw.trim().replace(",", ".");
  if (t === "") return null;
  const n = parseFloat(t);
  return Number.isNaN(n) ? null : n;
}

export function SettingsScreen({ onClose, onOpenHelp }: { onClose: () => void; onOpenHelp: () => void }) {
  const { state, setMesin, resetMesin, setThemeMode, exportJson, importJson } = useDoffStore();
  const { showToast, showConfirm } = useUiStore();
  const [search, setSearch] = useState("");
  const [showAll, setShowAll] = useState(false);
  const [activeMcNo, setActiveMcNo] = useState<string | null>(null);
  const [form, setForm] = useState<MesinData | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Grup per tipe mesin (urutan tetap) — bahasa ikon/warna yang sama dengan RadarCard,
  // supaya "mesin jenis apa ini" terbaca sama di semua layar. Pencarian cocok nomor
  // mesin saja (Master Blueprint v9.2 §4/§8), bukan corak lagi.
  const groupedEntries = useMemo(() => {
    const filtered = Object.entries(state.db).filter(([k, v]) => {
      if (!showAll && (v.corak === "" || v.corak === "-")) return false;
      if (search && !k.includes(search)) return false;
      return true;
    });
    return TIPE_LIST.map((tipe) => ({
      tipe,
      rows: filtered.filter(([, v]) => v.tipe === tipe).sort((a, b) => (parseInt(a[0], 10) || 0) - (parseInt(b[0], 10) || 0)),
    })).filter((g) => g.rows.length > 0);
  }, [state.db, search, showAll]);

  const unconfigured = useMemo(() => {
    const n = search.trim();
    if (!/^\d{1,3}$/.test(n)) return null;
    const mesin = state.db[n];
    if (mesin && (mesin.corak === "" || mesin.corak === "-")) return [n, mesin] as const;
    return null;
  }, [search, state.db]);

  function loadFrom(mcNo: string, mesin: MesinData) {
    setActiveMcNo(mcNo);
    setForm({ ...mesin });
  }

  function jumpToSearch() {
    const mesin = state.db[search];
    if (mesin) loadFrom(search, mesin);
  }

  function handleSave() {
    if (!activeMcNo || !form) return;
    const corak = form.corak.trim() || "-";
    setMesin(activeMcNo, { ...form, corak });
    showToast(`Mc ${activeMcNo} disimpan ✓`);
    setActiveMcNo(null);
    setForm(null);
    setSearch("");
  }

  function handleReset() {
    if (!activeMcNo) return;
    showConfirm(`Reset Mc ${activeMcNo} ke default? Corak, target yard, dan pengaturan lain akan dihapus.`, () => {
      resetMesin(activeMcNo);
      showToast(`Mc ${activeMcNo} direset ke default`);
      setActiveMcNo(null);
      setForm(null);
    });
  }

  function handleExport() {
    const json = exportJson();
    const stamp = new Date().toISOString().replace(/[-:]/g, "").replace("T", "-").slice(0, 13);
    const blob = new Blob([json], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `adoel-backup-${stamp}.json`;
    a.click();
    URL.revokeObjectURL(url);
    showToast("Data dicadangkan ✓");
  }

  function handleImportFile(file: File) {
    const reader = new FileReader();
    reader.onload = () => {
      const text = String(reader.result ?? "");
      showConfirm("Pulihkan dari file ini? Data yang ada sekarang akan ditimpa.", () => {
        const parsed = importJson(text);
        showToast(parsed ? "Data dipulihkan ✓" : "⚠ File bukan cadangan Adoel yang valid");
      });
    };
    reader.onerror = () => showToast("⚠ Gagal membaca file");
    reader.readAsText(file);
  }

  return (
    <div className="overlay">
      <div className="overlay-header">
        <h2>Pengaturan</h2>
        <button className="icon-btn" onClick={onClose} aria-label="Tutup">
          <CloseIcon />
        </button>
      </div>
      <div className="overlay-body" style={{ paddingBottom: 92 }}>
        <div className="field-label">Tema Aplikasi</div>
        <div style={{ display: "flex", gap: 8 }}>
          {(["SYSTEM", "DARK", "LIGHT"] as ThemeMode[]).map((m) => (
            <button
              key={m}
              className={`chip-btn${state.themeMode === m ? " active" : ""}`}
              onClick={() => setThemeMode(m)}
            >
              {m === "SYSTEM" ? "Sistem" : m === "DARK" ? "Gelap" : "Terang"}
            </button>
          ))}
        </div>

        <div style={{ height: 4 }} />
        <WovenDivider />

        <div className="field-label">Cadangan Data</div>
        <div style={{ fontSize: 12, color: "var(--text-muted)", marginTop: -6 }}>
          Cadangkan seluruh data (mesin, estimasi, riwayat doff, tema) ke file, atau pulihkan dari file cadangan.
        </div>
        <div className="btn-row">
          <button className="btn" onClick={handleExport}>
            Cadangkan
          </button>
          <button className="btn" style={{ color: "var(--cyan-500)" }} onClick={() => fileInputRef.current?.click()}>
            Pulihkan
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="application/json,.json"
            style={{ display: "none" }}
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) handleImportFile(file);
              e.target.value = "";
            }}
          />
        </div>

        <WovenDivider />

        <div className="field-label">Bantuan</div>
        <button className="btn" onClick={onOpenHelp}>
          Cara Pakai Adoel
        </button>

        <WovenDivider />

        <div className="field-label">Database Mesin</div>
        <label style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13, color: "var(--text-secondary)" }}>
          <input type="checkbox" checked={showAll} onChange={(e) => setShowAll(e.target.checked)} />
          Tampilkan semua (termasuk corak "-")
        </label>

        {unconfigured && (
          <button
            className="btn"
            style={{ color: "var(--cyan-500)", borderColor: "var(--cyan-500)" }}
            onClick={() => loadFrom(unconfigured[0], unconfigured[1])}
          >
            Konfigurasi Mc {unconfigured[0]} (belum diatur)
          </button>
        )}

        {groupedEntries.length === 0 && <div className="empty-state">Tidak ditemukan</div>}
        {groupedEntries.map(({ tipe, rows }) => (
          <div key={tipe}>
            <div className="mesin-group-head" style={{ color: TIPE_COLOR[tipe] }}>
              <MesinTipeIcon tipe={tipe} size={14} />
              <span>{tipe}</span>
              <span className="count">{rows.length}</span>
            </div>
            {rows.map(([k, v]) => (
              <div className="machine-list-item" key={k} onClick={() => loadFrom(k, v)} role="button">
                <span style={{ width: 32, fontWeight: 700 }}>{k}</span>
                <span style={{ flex: 1, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{v.corak}</span>
                {v.targetYard != null && <span style={{ fontSize: 11, color: "var(--text-faint)" }}>{v.targetYard}y</span>}
                <button>Edit</button>
              </div>
            ))}
          </div>
        ))}
      </div>

      {/* Bar konsol mengambang — cari & lompat ke satu mesin sekaligus (Master Blueprint
          v9.2 §8), menggantikan search bar biasa di atas. */}
      <div className="mesin-console floating-card">
        <div className="console-row">
          <input
            className="console-mcno-input"
            value={search}
            onChange={(e) => setSearch(e.target.value.replace(/\D/g, "").slice(0, 3))}
            onKeyDown={(e) => {
              if (e.key === "Enter") jumpToSearch();
            }}
            placeholder="Cari / edit nomor mesin"
            inputMode="numeric"
            autoComplete="off"
          />
          <button
            className="console-icon-btn"
            style={{
              background: search.trim() !== "" && state.db[search] ? "var(--cyan-600)" : "var(--bg-elevated-2)",
              color: search.trim() !== "" && state.db[search] ? "#fff" : "var(--text-faint)",
            }}
            disabled={search.trim() === "" || !state.db[search]}
            aria-label={`Edit Mc ${search}`}
            onClick={jumpToSearch}
          >
            <EditIcon size={18} />
          </button>
        </div>
      </div>

      {activeMcNo && form && (
        <MesinEditDialog
          key={activeMcNo}
          mcNo={activeMcNo}
          form={form}
          onChange={setForm}
          onClose={() => {
            setActiveMcNo(null);
            setForm(null);
          }}
          onSave={handleSave}
          onReset={handleReset}
        />
      )}
    </div>
  );
}

function MesinEditDialog({
  mcNo,
  form,
  onChange,
  onClose,
  onSave,
  onReset,
}: {
  mcNo: string;
  form: MesinData;
  onChange: (f: MesinData) => void;
  onClose: () => void;
  onSave: () => void;
  onReset: () => void;
}) {
  // State teks MENTAH untuk field angka (seperti targetYardText/speedText/koreksiText di
  // MesinEditPanel.kt) — supaya pengguna bisa mengetik desimal ("0.158" / "0,158") tanpa titik/
  // koma-nya kebuang saat diketik ulang dari nilai numerik. Di-seed sekali dari form saat mount
  // (dialog di-remount per mcNo lewat key parent), lalu jadi sumber kebenaran field teks.
  const [targetYardText, setTargetYardText] = useState(form.targetYard != null ? formatYard(form.targetYard) : "");
  const [speedText, setSpeedText] = useState(form.speed != null ? formatYard(form.speed) : "");
  const [koreksiText, setKoreksiText] = useState(form.koreksi != null ? formatYard(form.koreksi) : "");

  return (
    <div className="dialog-backdrop" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 400 }}>
        <div style={{ fontWeight: 800, fontSize: 20, marginBottom: 16 }}>Mc {mcNo}</div>

        <div className="field-label">Tipe Mesin</div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 16 }}>
          {TIPE_LIST.map((t) => (
            <button
              key={t}
              className={`chip-btn${form.tipe === t ? " active" : ""}`}
              onClick={() => onChange({ ...form, tipe: t })}
            >
              {t}
            </button>
          ))}
        </div>

        <div className="field-label">Corak</div>
        <input
          className="field-input"
          inputMode="numeric"
          value={form.corak}
          onChange={(e) => onChange({ ...form, corak: e.target.value })}
        />

        <div style={{ height: 12 }} />
        <div className="field-grid">
          <div>
            <div className="field-label">Target Yard</div>
            <input
              className="field-input"
              inputMode="decimal"
              placeholder="opsional"
              value={targetYardText}
              onChange={(e) => {
                setTargetYardText(e.target.value);
                onChange({ ...form, targetYard: parseMesinNum(e.target.value) });
              }}
            />
          </div>
          <div>
            <div className="field-label">Speed (D405)</div>
            <input
              className="field-input"
              inputMode="decimal"
              placeholder="opsional"
              value={speedText}
              onChange={(e) => {
                setSpeedText(e.target.value);
                onChange({ ...form, speed: parseMesinNum(e.target.value) });
              }}
            />
          </div>
        </div>

        <div style={{ height: 12 }} />
        <div className="field-label">Koreksi Menit (D408)</div>
        <input
          className="field-input"
          inputMode="decimal"
          placeholder="opsional"
          value={koreksiText}
          onChange={(e) => {
            setKoreksiText(e.target.value);
            onChange({ ...form, koreksi: parseMesinNum(e.target.value) });
          }}
        />

        <div className="actions" style={{ marginTop: 20, justifyContent: "space-between" }}>
          <button className="cancel" onClick={onReset} style={{ color: "var(--red-500)" }}>
            Reset
          </button>
          <div style={{ display: "flex", gap: 8 }}>
            <button className="cancel" onClick={onClose}>
              Batal
            </button>
            <button className="confirm" style={{ background: "var(--cyan-600)" }} onClick={onSave}>
              Simpan
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
