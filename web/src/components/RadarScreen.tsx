import { useMemo, useState } from "react";
import { useDoffStore } from "../store/DoffStore";
import { useUiStore } from "../store/UiStore";
import {
  BREAK_GAP_THRESHOLD_MIN,
  partitionSegeraMenunggu,
  sortedByNearest,
  urgencyLevel,
  type UrgencyLevel,
} from "../domain/estimasiUtils";
import { absMinToTimeStr, formatDeltaMin, formatYard, nowAbsMin } from "../domain/format";
import type { Estimasi } from "../domain/types";
import { DeleteIcon, CheckIcon } from "./Icons";
import { QuickEditDialog } from "./QuickEditDialog";

const URGENCY_COLOR: Record<UrgencyLevel, string> = {
  CALM: "var(--text-faint)",
  SOON: "var(--amber-400)",
  IMMINENT: "var(--orange-400)",
  OVERDUE: "var(--red-500)",
};

export function RadarScreen() {
  const { state, submitAktual, hapusEstimasi, restoreEstimasi } = useDoffStore();
  const { showToast, showConfirm } = useUiStore();
  const [filter, setFilter] = useState("");
  const [quickEditMcNo, setQuickEditMcNo] = useState<string | null>(null);
  const [, forceTick] = useState(0);

  // Re-render setiap 20 detik supaya "sisa waktu" & status OVERDUE tetap akurat
  // tanpa perlu interaksi pengguna.
  useInterval(() => forceTick((n) => n + 1), 20000);

  const nowAbs = nowAbsMin();
  const all = useMemo(() => sortedByNearest(state.estimasi), [state.estimasi]);
  const filtered = useMemo(() => {
    if (!filter.trim()) return all;
    const f = filter.trim().toLowerCase();
    return all.filter((e) => {
      const corak = e.corakOverride ?? state.db[e.mcNo]?.corak ?? "";
      return e.mcNo.toLowerCase().includes(f) || corak.toLowerCase().includes(f);
    });
  }, [all, filter, state.db]);
  const [segera, menunggu] = useMemo(() => partitionSegeraMenunggu(filtered, nowAbs), [filtered, nowAbs]);

  function handleHapus(mcNo: string) {
    showConfirm(`Hapus estimasi Mc ${mcNo}?`, () => {
      const est = state.estimasi[mcNo];
      hapusEstimasi(mcNo);
      showToast(`Mc ${mcNo} dihapus`, est ? () => restoreEstimasi(est) : undefined);
    });
  }

  function handleDoff(mcNo: string) {
    // Tap tombol doff di kartu radar setara dengan mengetik "<mcNo>" saja di mode
    // DOFFING lalu kirim — sama seperti handleDoff() di MainScreen.kt Android,
    // supaya dapat undo yang sama (mengembalikan entri + estimasi sebelumnya).
    const result = submitAktual(mcNo);
    if (result.ok) showToast(result.msg, result.undo);
    else showToast(`⚠ ${result.msg}`);
  }

  if (all.length === 0) {
    return (
      <div className="scroll-area">
        <div className="empty-state">Belum ada estimasi berjalan. Ketik perintah di bawah.</div>
      </div>
    );
  }

  return (
    <div className="scroll-area">
      {all.length > 4 && (
        <input
          className="filter-field"
          placeholder="Cari nomor mesin atau corak"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
      )}

      {filtered.length === 0 && <div className="empty-state">Tidak ditemukan</div>}

      {segera.length > 0 && (
        <>
          <div className="section-title" style={{ color: "var(--red-500)" }}>
            <span>Segera</span>
            <span className="count">{segera.length}</span>
          </div>
          {segera.map((est) => (
            <RadarCard
              key={est.mcNo}
              est={est}
              nowAbs={nowAbs}
              corak={est.corakOverride ?? state.db[est.mcNo]?.corak ?? "-"}
              targetYard={est.yardOverride ?? state.db[est.mcNo]?.targetYard ?? null}
              onDoff={() => handleDoff(est.mcNo)}
              onHapus={() => handleHapus(est.mcNo)}
              onTap={() => setQuickEditMcNo(est.mcNo)}
            />
          ))}
        </>
      )}

      {menunggu.length > 0 && (
        <>
          <div className="section-title" style={{ color: menungguAccent(menunggu, nowAbs) }}>
            <span>Menunggu</span>
            <span className="count">{menunggu.length}</span>
          </div>
          {menunggu.map((est, i) => {
            const next = menunggu[i + 1];
            const gap = next ? next.estAbsMin - est.estAbsMin : 0;
            return (
              <div key={est.mcNo}>
                <RadarCard
                  est={est}
                  nowAbs={nowAbs}
                  corak={est.corakOverride ?? state.db[est.mcNo]?.corak ?? "-"}
                  targetYard={est.yardOverride ?? state.db[est.mcNo]?.targetYard ?? null}
                  onDoff={() => handleDoff(est.mcNo)}
                  onHapus={() => handleHapus(est.mcNo)}
                  onTap={() => setQuickEditMcNo(est.mcNo)}
                />
                {next && gap >= BREAK_GAP_THRESHOLD_MIN && (
                  <div className="gap-row">
                    ⏸ jeda {formatDeltaMin(gap)} sampai Mc {next.mcNo}
                  </div>
                )}
              </div>
            );
          })}
        </>
      )}

      {quickEditMcNo && (
        <QuickEditDialog
          mcNo={quickEditMcNo}
          onClose={() => setQuickEditMcNo(null)}
        />
      )}
    </div>
  );
}

function menungguAccent(menunggu: Estimasi[], nowAbs: number): string {
  let worst: UrgencyLevel = "CALM";
  for (const e of menunggu) {
    const level = urgencyLevel(e.estAbsMin - nowAbs);
    if (rank(level) > rank(worst)) worst = level;
  }
  // "Segera" (OVERDUE) sudah punya bucket sendiri, jadi Menunggu paling parah IMMINENT.
  if (worst === "IMMINENT") return "var(--orange-400)";
  if (worst === "SOON") return "var(--amber-400)";
  return "var(--cyan-400)";
}
function rank(l: UrgencyLevel): number {
  return { CALM: 0, SOON: 1, IMMINENT: 2, OVERDUE: 3 }[l];
}

function RadarCard({
  est,
  nowAbs,
  corak,
  targetYard,
  onDoff,
  onHapus,
  onTap,
}: {
  est: Estimasi;
  nowAbs: number;
  corak: string;
  targetYard: number | null;
  onDoff: () => void;
  onHapus: () => void;
  onTap: () => void;
}) {
  const remaining = est.estAbsMin - nowAbs;
  const level = urgencyLevel(remaining);
  const sub = targetYard != null ? `${corak} · ${formatYard(targetYard)}y` : corak;

  return (
    <div
      className={`radar-card${level === "OVERDUE" ? " overdue" : ""}`}
      style={{ ["--urgency-color" as any]: URGENCY_COLOR[level] }}
    >
      <div className="main" onClick={onTap} role="button">
        <div className="mcno">Mc {est.mcNo}</div>
        <div className="corak">{sub}</div>
      </div>
      <div className="time">
        <div className="abs">{absMinToTimeStr(est.estAbsMin)}</div>
        <div className="rel">{remaining <= 0 ? `${formatDeltaMin(remaining)} lewat` : `${formatDeltaMin(remaining)} lagi`}</div>
      </div>
      <div className="actions">
        <button className="icon-btn" style={{ color: "var(--emerald-500)" }} onClick={onDoff} aria-label="Doff">
          <CheckIcon />
        </button>
        <button className="icon-btn" style={{ color: "var(--red-500)" }} onClick={onHapus} aria-label="Hapus">
          <DeleteIcon />
        </button>
      </div>
    </div>
  );
}

function useInterval(callback: () => void, delayMs: number) {
  useMemo(() => {
    const id = setInterval(callback, delayMs);
    return () => clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
}
