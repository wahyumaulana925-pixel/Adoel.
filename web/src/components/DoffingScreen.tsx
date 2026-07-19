import { useMemo, useState } from "react";
import { useDoffStore } from "../store/DoffStore";
import { useUiStore } from "../store/UiStore";
import { shareHistoryText, shareOrCopy } from "../domain/share";
import { TIPE_COLOR } from "../domain/mesinVisual";
import { useConsoleHandlers } from "../hooks/useConsoleHandlers";
import type { AktualEntry } from "../domain/types";
import { EditAktualDialog } from "./EditAktualDialog";
import { DeleteIcon, EditIcon, FlagIcon, MesinTipeIcon, ShareIcon } from "./Icons";

export function DoffingScreen() {
  const { state } = useDoffStore();
  const { showToast } = useUiStore();
  const { handleHapusAktual, handleFinishShift } = useConsoleHandlers();
  const [filter, setFilter] = useState("");
  const [editing, setEditing] = useState<AktualEntry | null>(null);

  // Terlama di atas — sama seperti aktualReversed di aplikasi Android: state.aktual
  // menyimpan terbaru di indeks 0 (di-prepend), jadi dibalik dulu supaya nomor urut
  // 1..N mencerminkan urutan doff SEBENARNYA di shift ini (bukan input filter).
  const chronological = useMemo(() => [...state.aktual].reverse(), [state.aktual]);
  const indexed = useMemo(() => chronological.map((entry, idx) => ({ entry, num: idx + 1 })), [chronological]);
  // Pencarian hanya nomor mesin (Master Blueprint v9.2 §4) — bukan corak/keterangan lagi.
  const filtered = useMemo(() => {
    if (!filter.trim()) return indexed;
    const f = filter.trim().toLowerCase();
    return indexed.filter(({ entry }) => entry.mcNo.toLowerCase().includes(f));
  }, [indexed, filter]);

  function handleHapus(id: number) {
    handleHapusAktual(id, () => setEditing(null));
  }

  async function handleShare() {
    const outcome = await shareOrCopy(shareHistoryText(state), "Riwayat Doffing");
    if (outcome === "copied") showToast("Teks disalin ke clipboard ✓");
  }

  return (
    <div className="scroll-area">
      <div className="section-title">
        <span>Doffing</span>
        <span className="count">{state.aktual.length}</span>
      </div>

      <div className="btn-row">
        <button className="btn" onClick={handleShare}>
          <ShareIcon size={16} /> Bagikan
        </button>
        <button className="btn danger" onClick={handleFinishShift}>
          <FlagIcon size={16} /> Selesai Shift
        </button>
      </div>

      {state.aktual.length === 0 ? (
        <div className="empty-state">Belum ada doff. Doff akan muncul di sini setelah kamu proses lewat konsol.</div>
      ) : (
        <>
          {chronological.length > 4 && (
            <input
              className="filter-field"
              placeholder="Cari nomor mesin"
              inputMode="numeric"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
            />
          )}
          {filtered.length === 0 ? (
            <div className="empty-state">Tidak ditemukan — coba nomor mesin lain</div>
          ) : (
            filtered.map(({ entry, num }) => {
              const mesin = state.db[entry.mcNo];
              const corak = entry.corakOverride ?? mesin?.corak ?? "—";
              const sub =
                entry.customYard != null
                  ? `${corak} · ${entry.customYard}y`
                  : mesin?.targetYard != null
                    ? `${corak} · ${mesin.targetYard}y`
                    : corak;
              return (
                <div className="doff-row" key={entry.id}>
                  <div className="num">{num}</div>
                  <div className="tipe-icon" style={{ color: mesin ? TIPE_COLOR[mesin.tipe] : "var(--text-faint)" }}>
                    {mesin ? <MesinTipeIcon tipe={mesin.tipe} size={16} /> : null}
                  </div>
                  <div className="main">
                    <div className="mcno">{entry.mcNo}</div>
                    <div className="sub">{sub}</div>
                  </div>
                  <div className="ket">{entry.ket}</div>
                  <div className="actions">
                    <button className="icon-btn" onClick={() => setEditing(entry)} aria-label="Edit">
                      <EditIcon size={16} />
                    </button>
                    <button
                      className="icon-btn"
                      style={{ color: "var(--red-500)" }}
                      onClick={() => handleHapus(entry.id)}
                      aria-label="Hapus"
                    >
                      <DeleteIcon size={16} />
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </>
      )}

      {editing && <EditAktualDialog entry={editing} onClose={() => setEditing(null)} onDelete={handleHapus} />}
    </div>
  );
}
