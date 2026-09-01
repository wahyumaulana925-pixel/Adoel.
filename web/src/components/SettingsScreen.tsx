import { useRef, useState } from "react";
import { useDoffStore } from "../store/DoffStore";
import { useUiStore } from "../store/UiStore";
import { DEFAULT_CORAK_SHORTCUTS, DEFAULT_KETERANGAN_SHORTCUTS } from "../domain/types";
import {
  AddIcon,
  BookOpenIcon,
  CloseIcon,
  DatabaseIcon,
  DeleteIcon,
  DownloadIcon,
  InfoIcon,
  MonitorIcon,
  MoonIcon,
  ResetIcon,
  SunIcon,
  TagIcon,
  TextureIcon,
  UploadIcon,
  WarningIcon,
} from "./Icons";
import { AboutDialog } from "./AboutDialog";

export function SettingsScreen({ onClose, onOpenHelp }: { onClose: () => void; onOpenHelp: () => void }) {
  const {
    state,
    resetDb,
    setThemeMode,
    exportJson,
    importJson,
    addKeteranganShortcut,
    removeKeteranganShortcut,
    resetKeteranganShortcuts,
    addCorakShortcut,
    removeCorakShortcut,
    resetCorakShortcuts,
  } = useDoffStore();
  const { showToast, showConfirm } = useUiStore();
  const [aboutOpen, setAboutOpen] = useState(false);
  const [newShortcut, setNewShortcut] = useState("");
  const [newCorakShortcut, setNewCorakShortcut] = useState("");
  const fileInputRef = useRef<HTMLInputElement>(null);

  const shortcuts = state.keteranganShortcuts ?? DEFAULT_KETERANGAN_SHORTCUTS;
  const corakShortcuts = state.corakShortcuts ?? DEFAULT_CORAK_SHORTCUTS;

  function handleAddShortcut() {
    const trimmed = newShortcut.trim().toUpperCase();
    if (!trimmed) return;
    if (shortcuts.includes(trimmed)) {
      showToast(`Shortcut "${trimmed}" sudah ada di daftar`);
      return;
    }
    addKeteranganShortcut(trimmed);
    setNewShortcut("");
    showToast(`Shortcut "${trimmed}" ditambahkan ✓`);
  }

  function handleAddCorakShortcut() {
    const trimmed = newCorakShortcut.trim().toUpperCase();
    if (!trimmed) return;
    if (corakShortcuts.includes(trimmed)) {
      showToast(`Shortcut corak "${trimmed}" sudah ada di daftar`);
      return;
    }
    addCorakShortcut(trimmed);
    setNewCorakShortcut("");
    showToast(`Shortcut corak "${trimmed}" ditambahkan ✓`);
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

  function handleResetAllDb() {
    showConfirm("Reset semua data ke default? Estimasi & riwayat akan hilang.", () => {
      resetDb();
      showToast("Data direset ke default");
    });
  }

  return (
    <div className="overlay">
      <div className="overlay-header">
        <h2>Pengaturan</h2>
        <button className="icon-btn" onClick={onClose} aria-label="Tutup">
          <CloseIcon />
        </button>
      </div>

      <div className="overlay-body" style={{ paddingBottom: 32 }}>
        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          {/* Card: Tema Aplikasi */}
          <div className="settings-section-card">
            <div className="settings-section-header">
              <SunIcon size={16} />
              <span>Tema Tampilan</span>
            </div>
            <div className="settings-section-desc">Pilih tema antarmuka yang nyaman untuk operasional kerja.</div>
            <div className="settings-theme-selector">
              <button
                className={`settings-theme-btn${state.themeMode === "SYSTEM" ? " active" : ""}`}
                onClick={() => setThemeMode("SYSTEM")}
              >
                <MonitorIcon size={15} />
                <span>Sistem</span>
              </button>
              <button
                className={`settings-theme-btn${state.themeMode === "DARK" ? " active" : ""}`}
                onClick={() => setThemeMode("DARK")}
              >
                <MoonIcon size={15} />
                <span>Gelap</span>
              </button>
              <button
                className={`settings-theme-btn${state.themeMode === "LIGHT" ? " active" : ""}`}
                onClick={() => setThemeMode("LIGHT")}
              >
                <SunIcon size={15} />
                <span>Terang</span>
              </button>
            </div>
          </div>

          {/* Card: Shortcut Keterangan Doffing */}
          <div className="settings-section-card">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div className="settings-section-header" style={{ margin: 0, display: "flex", alignItems: "center", gap: 8 }}>
                <div style={{ padding: 6, borderRadius: 6, background: "rgba(6, 182, 212, 0.15)", color: "var(--cyan-400)", display: "flex" }}>
                  <TagIcon size={16} />
                </div>
                <span>Shortcut Keterangan</span>
                <span className="meta-tag" style={{ marginLeft: 4 }}>
                  {shortcuts.length} shortcut
                </span>
              </div>
              {shortcuts.length > 0 && (
                <button
                  type="button"
                  className="btn-link"
                  style={{ fontSize: 12, color: "var(--red-400)", display: "inline-flex", alignItems: "center", gap: 3 }}
                  onClick={() => {
                    showConfirm("Hapus semua daftar shortcut keterangan?", () => {
                      resetKeteranganShortcuts();
                      showToast("Daftar shortcut keterangan dikosongkan ✓");
                    });
                  }}
                >
                  <DeleteIcon size={12} />
                  <span>Hapus Semua</span>
                </button>
              )}
            </div>
            <div className="settings-section-desc" style={{ marginTop: 8 }}>
              Tombol cepat keterangan untuk pencatatan Doffing (cth: HB, P.LP, P.SN, GANTI BEAM).
            </div>

            {shortcuts.length === 0 ? (
              <div
                style={{
                  border: "1px dashed var(--border-subtle)",
                  borderRadius: 8,
                  padding: "12px 14px",
                  margin: "8px 0",
                  textAlign: "center",
                  fontSize: 12,
                  color: "var(--text-faint)",
                  background: "var(--bg-elevated)",
                }}
              >
                Belum ada shortcut keterangan. Tambahkan teks di bawah untuk membuat tombol cepat.
              </div>
            ) : (
              <div className="chip-row-wrap" style={{ marginTop: 8, marginBottom: 4 }}>
                {shortcuts.map((code) => (
                  <div
                    key={code}
                    style={{
                      display: "inline-flex",
                      alignItems: "center",
                      background: "var(--bg-elevated-2)",
                      border: "1px solid var(--border-subtle)",
                      borderRadius: 6,
                      padding: "3px 6px 3px 10px",
                      gap: 6,
                      fontSize: 12,
                      fontWeight: 600,
                      color: "var(--text-primary)",
                    }}
                  >
                    <span>{code}</span>
                    <button
                      type="button"
                      aria-label={`Hapus shortcut ${code}`}
                      title={`Hapus shortcut ${code}`}
                      onClick={() => {
                        removeKeteranganShortcut(code);
                        showToast(`Shortcut "${code}" dihapus`);
                      }}
                      style={{
                        background: "transparent",
                        color: "var(--text-faint)",
                        border: "none",
                        borderRadius: "50%",
                        width: 18,
                        height: 18,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        cursor: "pointer",
                        padding: 0,
                        fontSize: 12,
                        lineHeight: 1,
                        transition: "color 0.15s, background 0.15s",
                      }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.color = "#ef4444";
                        e.currentTarget.style.background = "rgba(239, 68, 68, 0.15)";
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.color = "var(--text-faint)";
                        e.currentTarget.style.background = "transparent";
                      }}
                    >
                      ✕
                    </button>
                  </div>
                ))}
              </div>
            )}

            <div style={{ display: "flex", gap: 6, marginTop: 10 }}>
              <input
                className="field-input"
                style={{ flex: 1, padding: "8px 12px", fontSize: 13 }}
                placeholder="Tambah keterangan baru (cth: GANTI BEAM)"
                value={newShortcut}
                onChange={(e) => setNewShortcut(e.target.value.toUpperCase())}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    handleAddShortcut();
                  }
                }}
              />
              <button
                className="btn primary"
                style={{ padding: "8px 16px", fontSize: 13, display: "inline-flex", alignItems: "center", gap: 4 }}
                disabled={!newShortcut.trim()}
                onClick={handleAddShortcut}
              >
                <AddIcon size={14} />
                <span>Tambah</span>
              </button>
            </div>
          </div>

          {/* Card: Shortcut Kode Corak */}
          <div className="settings-section-card">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div className="settings-section-header" style={{ margin: 0, display: "flex", alignItems: "center", gap: 8 }}>
                <div style={{ padding: 6, borderRadius: 6, background: "rgba(16, 185, 129, 0.15)", color: "var(--emerald-400)", display: "flex" }}>
                  <TextureIcon size={16} />
                </div>
                <span>Shortcut Kode Corak</span>
                <span className="meta-tag" style={{ marginLeft: 4 }}>
                  {corakShortcuts.length} shortcut
                </span>
              </div>
              {corakShortcuts.length > 0 && (
                <button
                  type="button"
                  className="btn-link"
                  style={{ fontSize: 12, color: "var(--red-400)", display: "inline-flex", alignItems: "center", gap: 3 }}
                  onClick={() => {
                    showConfirm("Hapus semua daftar shortcut kode corak?", () => {
                      resetCorakShortcuts();
                      showToast("Daftar shortcut corak dikosongkan ✓");
                    });
                  }}
                >
                  <DeleteIcon size={12} />
                  <span>Hapus Semua</span>
                </button>
              )}
            </div>
            <div className="settings-section-desc" style={{ marginTop: 8 }}>
              Tombol cepat kode corak/kain untuk formulir mesin dan penggantian corak (cth: 4500, 4505, 5000, RAYON-30).
            </div>

            {corakShortcuts.length === 0 ? (
              <div
                style={{
                  border: "1px dashed var(--border-subtle)",
                  borderRadius: 8,
                  padding: "12px 14px",
                  margin: "8px 0",
                  textAlign: "center",
                  fontSize: 12,
                  color: "var(--text-faint)",
                  background: "var(--bg-elevated)",
                }}
              >
                Belum ada shortcut kode corak. Tambahkan kode corak di bawah untuk membuat tombol cepat.
              </div>
            ) : (
              <div className="chip-row-wrap" style={{ marginTop: 8, marginBottom: 4 }}>
                {corakShortcuts.map((code) => (
                  <div
                    key={code}
                    style={{
                      display: "inline-flex",
                      alignItems: "center",
                      background: "var(--bg-elevated-2)",
                      border: "1px solid var(--border-subtle)",
                      borderRadius: 6,
                      padding: "3px 6px 3px 10px",
                      gap: 6,
                      fontSize: 12,
                      fontWeight: 600,
                      color: "var(--text-primary)",
                    }}
                  >
                    <span>{code}</span>
                    <button
                      type="button"
                      aria-label={`Hapus shortcut corak ${code}`}
                      title={`Hapus shortcut corak ${code}`}
                      onClick={() => {
                        removeCorakShortcut(code);
                        showToast(`Shortcut corak "${code}" dihapus`);
                      }}
                      style={{
                        background: "transparent",
                        color: "var(--text-faint)",
                        border: "none",
                        borderRadius: "50%",
                        width: 18,
                        height: 18,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        cursor: "pointer",
                        padding: 0,
                        fontSize: 12,
                        lineHeight: 1,
                        transition: "color 0.15s, background 0.15s",
                      }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.color = "#ef4444";
                        e.currentTarget.style.background = "rgba(239, 68, 68, 0.15)";
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.color = "var(--text-faint)";
                        e.currentTarget.style.background = "transparent";
                      }}
                    >
                      ✕
                    </button>
                  </div>
                ))}
              </div>
            )}

            <div style={{ display: "flex", gap: 6, marginTop: 10 }}>
              <input
                className="field-input"
                style={{ flex: 1, padding: "8px 12px", fontSize: 13 }}
                placeholder="Tambah kode corak baru (cth: 4520 / RAYON)"
                value={newCorakShortcut}
                onChange={(e) => setNewCorakShortcut(e.target.value.toUpperCase())}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    handleAddCorakShortcut();
                  }
                }}
              />
              <button
                className="btn primary"
                style={{ padding: "8px 16px", fontSize: 13, display: "inline-flex", alignItems: "center", gap: 4 }}
                disabled={!newCorakShortcut.trim()}
                onClick={handleAddCorakShortcut}
              >
                <AddIcon size={14} />
                <span>Tambah</span>
              </button>
            </div>
          </div>

          {/* Card: Cadangan Data */}
          <div className="settings-section-card">
            <div className="settings-section-header">
              <DatabaseIcon size={16} />
              <span>Cadangan &amp; Pemulihan</span>
            </div>
            <div className="settings-section-desc">
              Simpan seluruh data database mesin, estimasi aktif, dan riwayat shift ke file JSON cadangan.
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
              <button className="settings-action-btn primary" onClick={handleExport}>
                <DownloadIcon size={16} />
                <span>Cadangkan</span>
              </button>
              <button className="settings-action-btn" onClick={() => fileInputRef.current?.click()}>
                <UploadIcon size={16} />
                <span>Pulihkan</span>
              </button>
            </div>
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

          {/* Card: Zona Reset */}
          <div className="settings-section-card">
            <div className="settings-section-header danger">
              <WarningIcon size={16} />
              <span>Reset Data</span>
            </div>
            <div className="settings-section-desc">
              Mengembalikan pengaturan database mesin ke bawaan pabrik dan menghapus seluruh riwayat shift.
            </div>
            <button className="settings-action-btn danger" onClick={handleResetAllDb}>
              <ResetIcon size={16} />
              <span>Reset Semua ke Default</span>
            </button>
          </div>

          {/* Card: Bantuan & Info */}
          <div className="settings-section-card">
            <div className="settings-section-header">
              <InfoIcon size={16} />
              <span>Bantuan &amp; Informasi</span>
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
              <button className="settings-action-btn" onClick={onOpenHelp}>
                <BookOpenIcon size={16} />
                <span>Panduan</span>
              </button>
              <button className="settings-action-btn" onClick={() => setAboutOpen(true)}>
                <InfoIcon size={16} />
                <span>Tentang</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {aboutOpen && <AboutDialog onClose={() => setAboutOpen(false)} />}
    </div>
  );
}
