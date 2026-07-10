import { useMemo, useState } from "react";
import { useDoffStore } from "../store/DoffStore";
import { useUiStore } from "../store/UiStore";
import { shareHistoryText, shareOrCopy } from "../domain/share";
import type { AktualEntry } from "../domain/types";
import { EditAktualDialog } from "./EditAktualDialog";
import { BarChartIcon, DeleteIcon, EditIcon, FlagIcon, ShareIcon } from "./Icons";

const TIPE_COLOR: Record<string, string> = {
  TAPPET: "#14b8a6",
  CAM: "#8b5cf6",
  D405: "#f59e0b",
  D408: "#0ea5e9",
};

export function DoffingScreen({ onOpenStatistik }: { onOpenStatistik: () => void }) {
  const { state, hapusAktualById, restoreAktual, finishShift } = useDoffStore();
  const { showToast, showConfirm } = useUiStore();
  const [filter, setFilter] = useState("");
  const [editing, setEditing] = useState<AktualEntry | null>(null);

  // Terlama di atas — sama seperti aktualReversed di aplikasi Android: state.aktual
  // menyimpan terbaru di indeks 0 (di-prepend), jadi dibalik dulu supaya nomor urut
  // 1..N mencerminkan urutan doff SEBENARNYA di shift ini (bukan input filter).
  const chronological = useMemo(() => [...state.aktual].reverse(), [state.aktual]);
  const indexed = useMemo(() => chronological.map((entry, idx) => ({ entry, num: idx + 1 })), [chronological]);
  const filtered = useMemo(() => {
    if (!filter.trim()) return indexed;
    const f = filter.trim().toLowerCase();
    return indexed.filter(({ entry }) => {
      const corak = entry.corakOverride ?? state.db[entry.mcNo]?.corak ?? "";
      return (
        entry.mcNo.toLowerCase().includes(f) ||
        corak.toLowerCase().includes(f) ||
        entry.ket.toLowerCase().includes(f)
      );
    });
  }, [indexed, filter, state.db]);

  function handleHapus(id: number) {
    const entry = state.aktual.find((a) => a.id === id);
    if (!entry) return;
    showConfirm(`Hapus riwayat Mc ${entry.mcNo}?`, () => {
      hapusAktualById(id);
      showToast(`Mc ${entry.mcNo} dihapus`, () => restoreAktual(entry));
    });
  }

  async function handleShare() {
    const outcome = await shareOrCopy(shareHistoryText(state), "Riwayat Doffing");
    if (outcome === "copied") showToast("Teks disalin ke clipboard ✓");
  }

  function handleFinish() {
    if (state.aktual.length === 0 && Object.keys(state.estimasi).length === 0) {
      showToast("Tidak ada yang perlu diarsipkan");
      return;
    }
    showConfirm("Akhiri shift? Semua riwayat & estimasi berjalan akan diarsipkan ke Statistik.", () => {
      finishShift();
      showToast("Shift selesai ✓");
    });
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
        <button className="btn" onClick={onOpenStatistik}>
          <BarChartIcon size={16} /> Statistik
        </button>
        <button className="btn danger" onClick={handleFinish}>
          <FlagIcon size={16} /> Selesai Shift
        </button>
      </div>

      {state.aktual.length === 0 ? (
        <div className="empty-state">Belum ada doff. Doff akan muncul di sini setelah kamu proses baris di ESTIMASI/AKTUAL.</div>
      ) : (
        <>
          {chronological.length > 4 && (
            <input
              className="filter-field"
              placeholder="Cari nomor mesin, corak, atau keterangan"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
            />
          )}
          {filtered.length === 0 ? (
            <div className="empty-state">Tidak ditemukan — coba kata kunci lain</div>
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
                  <div className="dot" style={{ background: TIPE_COLOR[mesin?.tipe ?? "TAPPET"] }} />
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
