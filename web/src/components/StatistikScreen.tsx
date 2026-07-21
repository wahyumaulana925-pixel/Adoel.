import { useMemo, useState } from "react";
import { useDoffStore } from "../store/DoffStore";
import { useUiStore } from "../store/UiStore";
import { formatDeltaMin, shiftNumberForEpochMin } from "../domain/format";
import { sortAktualChronological } from "../domain/aktualOrder";
import { shareOrCopy, shareShiftText } from "../domain/share";
import { TIPE_COLOR } from "../domain/mesinVisual";
import type { MesinTipe, ShiftRecord } from "../domain/types";
import { CloseIcon, DeleteIcon, ShareIcon } from "./Icons";
import { WaveProgressBar } from "./WaveProgressBar";
import { WovenDivider } from "./WovenDivider";

function formatShiftDate(epochMin: number): string {
  const d = new Date(epochMin * 60000);
  return `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}/${d.getFullYear()}`;
}
function formatShiftTime(epochMin: number): string {
  const d = new Date(epochMin * 60000);
  return `${pad2(d.getHours())}.${pad2(d.getMinutes())}`;
}
function pad2(n: number): string {
  return n.toString().padStart(2, "0");
}

export function StatistikScreen({ onClose }: { onClose: () => void }) {
  const { state, hapusShift } = useDoffStore();
  const { showConfirm, showToast } = useUiStore();
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const totalDoff = useMemo(() => state.history.reduce((sum, h) => sum + h.aktual.length, 0), [state.history]);
  const avgPerShift = state.history.length > 0 ? totalDoff / state.history.length : 0;
  const maxDoffCount = useMemo(() => Math.max(1, ...state.history.map((h) => h.aktual.length)), [state.history]);

  // Breakdown jumlah doff per tipe mesin, urutan tetap TAPPET→CAM→D405→D408; hanya tipe dengan
  // count > 0 yang ditampilkan — port dari TipeBreakdownBar di StatistikScreen.kt.
  const tipeCounts = useMemo(() => {
    const order: MesinTipe[] = ["TAPPET", "CAM", "D405", "D408"];
    const byTipe: Partial<Record<MesinTipe, number>> = {};
    for (const h of state.history) {
      for (const a of h.aktual) {
        const tipe = state.db[a.mcNo]?.tipe;
        if (tipe) byTipe[tipe] = (byTipe[tipe] ?? 0) + 1;
      }
    }
    return order.filter((t) => (byTipe[t] ?? 0) > 0).map((t) => ({ tipe: t, count: byTipe[t]! }));
  }, [state.history, state.db]);

  function handleDelete(shift: ShiftRecord) {
    const shiftNo = shiftNumberForEpochMin(shift.startedAtEpochMin);
    const dateStr = formatShiftDate(shift.startedAtEpochMin);
    showConfirm(`Hapus arsip Shift ${shiftNo} · ${dateStr}? Data ini tidak bisa dikembalikan.`, () => {
      hapusShift(shift.id);
    });
  }

  async function handleShare(shift: ShiftRecord) {
    if (shift.aktual.length === 0) return;
    const outcome = await shareOrCopy(shareShiftText(shift, state.db), "Riwayat Shift");
    if (outcome === "copied") showToast("Teks disalin ke clipboard ✓");
  }

  return (
    <div className="overlay">
      <div className="overlay-header">
        <h2>Statistik</h2>
        <button className="icon-btn" onClick={onClose} aria-label="Tutup">
          <CloseIcon />
        </button>
      </div>
      <div className="overlay-body">
        <div className="card" style={{ display: "flex", justifyContent: "space-around", textAlign: "center" }}>
          <div>
            <div style={{ fontSize: 22, fontWeight: 800, color: "var(--cyan-400)" }}>{totalDoff}</div>
            <div style={{ fontSize: 11, color: "var(--text-faint)" }}>Total Doff</div>
          </div>
          <div>
            <div style={{ fontSize: 22, fontWeight: 800, color: "var(--cyan-400)" }}>{avgPerShift.toFixed(1)}</div>
            <div style={{ fontSize: 11, color: "var(--text-faint)" }}>Rata-rata/Shift</div>
          </div>
          <div>
            <div style={{ fontSize: 22, fontWeight: 800, color: "var(--cyan-400)" }}>{state.history.length}</div>
            <div style={{ fontSize: 11, color: "var(--text-faint)" }}>Shift Diarsipkan</div>
          </div>
        </div>

        {tipeCounts.length > 0 && (
          <div className="card">
            <div style={{ display: "flex", height: 6, borderRadius: 3, overflow: "hidden" }}>
              {tipeCounts.map(({ tipe, count }) => (
                <div key={tipe} style={{ flexGrow: count, minWidth: 2, background: TIPE_COLOR[tipe] }} />
              ))}
            </div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 12, marginTop: 8 }}>
              {tipeCounts.map(({ tipe, count }) => (
                <div key={tipe} style={{ display: "flex", alignItems: "center", gap: 4 }}>
                  <span style={{ width: 6, height: 6, borderRadius: "50%", background: TIPE_COLOR[tipe], display: "inline-block" }} />
                  <span style={{ fontSize: 12, color: "var(--text-faint)" }}>
                    {tipe} {count}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        <WovenDivider />

        {state.history.length === 0 ? (
          <div className="empty-state">Belum ada shift yang diarsipkan. Tekan "Selesai Shift" untuk mengarsipkan.</div>
        ) : (
          state.history.map((shift) => (
            <ShiftCard
              key={shift.id}
              shift={shift}
              maxDoffCount={maxDoffCount}
              expanded={expandedId === shift.id}
              onToggle={() => setExpandedId(expandedId === shift.id ? null : shift.id)}
              onDelete={() => handleDelete(shift)}
              onShare={() => handleShare(shift)}
              db={state.db}
            />
          ))
        )}
      </div>
    </div>
  );
}

function ShiftCard({
  shift,
  maxDoffCount,
  expanded,
  onToggle,
  onDelete,
  onShare,
  db,
}: {
  shift: ShiftRecord;
  maxDoffCount: number;
  expanded: boolean;
  onToggle: () => void;
  onDelete: () => void;
  onShare: () => void;
  db: ReturnType<typeof useDoffStore>["state"]["db"];
}) {
  const shiftNo = shiftNumberForEpochMin(shift.startedAtEpochMin);
  const dateStr = formatShiftDate(shift.startedAtEpochMin);
  const timeRange = `${formatShiftTime(shift.startedAtEpochMin)}–${formatShiftTime(shift.endedAtEpochMin)}`;
  // +240 (4 jam setelah mulai) dipakai sebagai titik tengah yang aman dari pembungkusan
  // tanggal untuk shift 8 jam manapun — shift ini sudah diarsipkan, bisa dibuka
  // berhari-hari kemudian, jadi "sekarang" bukan acuan yang masuk akal.
  const chronological = useMemo(
    () => sortAktualChronological(shift.aktual, shift.startedAtEpochMin + 240),
    [shift.aktual, shift.startedAtEpochMin],
  );
  const avgGapMin = useMemo(() => {
    const stamped = chronological.map((a) => a.tsEpochMin).filter((t): t is number => t !== null);
    if (stamped.length < 2) return null;
    let total = 0;
    for (let i = 1; i < stamped.length; i++) total += stamped[i] - stamped[i - 1];
    return total / (stamped.length - 1);
  }, [chronological]);

  return (
    <div className="shift-card">
      <div className="top" onClick={onToggle} role="button">
        <div>
          <div className="title">
            Shift {shiftNo} · {dateStr}
          </div>
          <div className="sub">{timeRange}</div>
          <div style={{ width: 60, marginTop: 6 }}>
            <WaveProgressBar
              fraction={shift.aktual.length / maxDoffCount}
              trackColor="var(--bg-elevated-2)"
              fillColor="var(--cyan-500)"
              height={4}
              animated={false}
            />
          </div>
        </div>
        <div className="stats">
          <div className="num">{shift.aktual.length} doff</div>
          {avgGapMin != null && <div className="gap">±{formatDeltaMin(Math.round(avgGapMin))}/doff</div>}
        </div>
      </div>
      <div className="btn-row" style={{ marginTop: 10 }}>
        <button className="btn" onClick={onShare} disabled={shift.aktual.length === 0}>
          <ShareIcon size={14} /> Bagikan
        </button>
        <button className="btn danger" onClick={onDelete}>
          <DeleteIcon size={14} /> Hapus
        </button>
      </div>
      {expanded && (
        <div className="shift-detail">
          {chronological.map((entry) => {
            const mesin = db[entry.mcNo];
            const corak = entry.corakOverride ?? mesin?.corak ?? "—";
            const yard = entry.customYard ?? mesin?.targetYard;
            const corakLine = yard != null ? `${corak} · ${yard}y` : corak;
            return (
              <div className="row" key={entry.id}>
                <span>
                  Mc {entry.mcNo} · {corakLine} · {entry.ket}
                </span>
                <span className="jam">{entry.jam}</span>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
