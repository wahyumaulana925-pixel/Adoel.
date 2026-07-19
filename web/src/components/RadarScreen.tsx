import { useEffect, useMemo, useState } from "react";
import { useDoffStore } from "../store/DoffStore";
import { useConsoleHandlers } from "../hooks/useConsoleHandlers";
import {
  BREAK_GAP_THRESHOLD_MIN,
  effectiveRemaining,
  partitionSegeraMenunggu,
  sortedByNearest,
  urgencyLevel,
  type UrgencyLevel,
} from "../domain/estimasiUtils";
import { formatDeltaMin, nowAbsMin } from "../domain/format";
import type { Estimasi } from "../domain/types";
import { RadarCard } from "./RadarCard";
import { QuickEditDialog } from "./QuickEditDialog";

export function RadarScreen({ onEditWaktu }: { onEditWaktu: (mcNo: string) => void }) {
  const { state } = useDoffStore();
  const { handleDoff, handleHapusEst, handleJeda, handleLanjutkan } = useConsoleHandlers();
  const [filter, setFilter] = useState("");
  const [quickEditMcNo, setQuickEditMcNo] = useState<string | null>(null);
  const [, forceTick] = useState(0);

  // Re-render setiap 20 detik supaya "sisa waktu" & status OVERDUE tetap akurat
  // tanpa perlu interaksi pengguna. Pakai useEffect (bukan useMemo) supaya interval
  // benar-benar dibersihkan saat unmount — kalau tidak, tiap kali layar Radar dibuka
  // ulang akan menumpuk interval baru yang tidak pernah berhenti.
  useEffect(() => {
    const id = setInterval(() => forceTick((n) => n + 1), 20000);
    return () => clearInterval(id);
  }, []);

  const nowAbs = nowAbsMin();
  const all = useMemo(() => sortedByNearest(state.estimasi), [state.estimasi]);
  // Pencarian hanya nomor mesin (Master Blueprint v9.2 §4) — bukan corak lagi.
  const filtered = useMemo(() => {
    if (!filter.trim()) return all;
    const f = filter.trim().toLowerCase();
    return all.filter((e) => e.mcNo.toLowerCase().includes(f));
  }, [all, filter]);
  const [segera, menunggu] = useMemo(() => partitionSegeraMenunggu(filtered, nowAbs), [filtered, nowAbs]);

  if (all.length === 0) {
    return (
      <div className="scroll-area">
        <div className="empty-state">Belum ada estimasi berjalan. Ketik nomor mesin di bawah, lalu ketuk jam (estimasi).</div>
      </div>
    );
  }

  return (
    <div className="scroll-area">
      {all.length > 4 && (
        <input
          className="filter-field"
          placeholder="Cari nomor mesin"
          inputMode="numeric"
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
              mesin={state.db[est.mcNo] ?? null}
              nowAbs={nowAbs}
              onDoff={() => handleDoff(est.mcNo)}
              onDoffMatching={() => handleDoff(est.mcNo, "MATCHING")}
              onHapus={() => handleHapusEst(est.mcNo)}
              onJeda={() => handleJeda(est.mcNo)}
              onLanjutkan={() => handleLanjutkan(est.mcNo)}
              onQuickEdit={() => setQuickEditMcNo(est.mcNo)}
              onEditWaktu={() => onEditWaktu(est.mcNo)}
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
          {/* Leading break: kalau tidak ada yang overdue (Segera kosong) dan mesin terdekat
              masih >= 30 menit lagi, tandai operator boleh istirahat DARI SEKARANG sampai Mc itu
              — port dari leading break di MainScreen.kt. Diukur dari nowAbs (bukan antar-dua
              estimasi seperti gap-row di bawah). */}
          {segera.length === 0 && menunggu[0] && menunggu[0].estAbsMin - nowAbs >= BREAK_GAP_THRESHOLD_MIN && (
            <div className="gap-row">
              ⏸ bisa istirahat {formatDeltaMin(menunggu[0].estAbsMin - nowAbs)} sampai Mc {menunggu[0].mcNo}
            </div>
          )}
          {menunggu.map((est, i) => {
            const next = menunggu[i + 1];
            const gap = next ? next.estAbsMin - est.estAbsMin : 0;
            return (
              <div key={est.mcNo}>
                <RadarCard
                  est={est}
                  mesin={state.db[est.mcNo] ?? null}
                  nowAbs={nowAbs}
                  onDoff={() => handleDoff(est.mcNo)}
                  onDoffMatching={() => handleDoff(est.mcNo, "MATCHING")}
                  onHapus={() => handleHapusEst(est.mcNo)}
                  onJeda={() => handleJeda(est.mcNo)}
                  onLanjutkan={() => handleLanjutkan(est.mcNo)}
                  onQuickEdit={() => setQuickEditMcNo(est.mcNo)}
                  onEditWaktu={() => onEditWaktu(est.mcNo)}
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

      {quickEditMcNo && <QuickEditDialog mcNo={quickEditMcNo} onClose={() => setQuickEditMcNo(null)} />}
    </div>
  );
}

function menungguAccent(menunggu: Estimasi[], nowAbs: number): string {
  let worst: UrgencyLevel = "CALM";
  for (const e of menunggu) {
    const level = urgencyLevel(effectiveRemaining(e, nowAbs));
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
