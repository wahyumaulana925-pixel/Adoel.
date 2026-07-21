// Port 1:1 dari shareHistory (DoffingSection.kt) dan shareShift (StatistikScreen.kt).
import { absMinToTimeStr, formatYard, nowAbsMin, shiftNumberForEpochMin } from "./format";
import { sortedByNearest } from "./estimasiUtils";
import { sortAktualChronological } from "./aktualOrder";
import type { DoffState, MesinData, ShiftRecord } from "./types";

function dateStrNow(): string {
  const d = new Date();
  return `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}/${d.getFullYear()}`;
}

function formatShiftDate(epochMin: number): string {
  const d = new Date(epochMin * 60000);
  return `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}/${d.getFullYear()}`;
}

function pad2(n: number): string {
  return n.toString().padStart(2, "0");
}

/** Teks ringkasan siap-bagikan untuk daftar Doffing shift berjalan — termasuk daftar
 * mesin yang masih berjalan (estimasi aktif), karena rekan yang baca pesan ini di
 * lantai produksi juga perlu tahu mesin mana yang belum di-doff. */
export function shareHistoryText(state: DoffState): string {
  const dateStr = dateStrNow();
  const aktualChrono = sortAktualChronological(state.aktual, nowAbsMin());
  const lines = aktualChrono.map((a, i) => {
    const mesin = state.db[a.mcNo];
    const corak = a.corakOverride ?? mesin?.corak ?? "—";
    const yard = a.customYard ?? mesin?.targetYard;
    const suffix = yard != null ? ` · ${formatYard(yard)}y` : "";
    return `${i + 1}. Mc${a.mcNo} · ${corak}${suffix} · ${a.ket}`;
  });
  const berjalan = sortedByNearest(state.estimasi).map((est) => {
    const mesin = state.db[est.mcNo];
    const corak = est.corakOverride ?? mesin?.corak ?? "—";
    const yard = est.yardOverride ?? mesin?.targetYard;
    const suffix = yard != null ? ` · ${formatYard(yard)}y` : "";
    return `• Mc${est.mcNo} · ${corak}${suffix} · est. ${absMinToTimeStr(est.estAbsMin)}`;
  });
  const selesaiCount = state.aktual.length;
  const berjalanCount = berjalan.length;
  const totalLine =
    berjalanCount > 0
      ? `Total: ${selesaiCount} selesai + ${berjalanCount} berjalan = ${selesaiCount + berjalanCount} mc`
      : `Total: ${selesaiCount} doff`;
  const berjalanBlock = berjalan.length > 0 ? `\n\n*Sedang Berjalan (${berjalanCount})*\n${berjalan.join("\n")}` : "";
  return `Bravo!!!\n${dateStr}\n\n*Selesai (${selesaiCount} doff)*\n${lines.join("\n")}${berjalanBlock}\n\n${totalLine}`;
}

/** Teks ringkasan siap-bagikan untuk satu shift yang sudah diarsipkan. */
export function shareShiftText(shift: ShiftRecord, db: Record<string, MesinData>): string {
  const shiftNo = shiftNumberForEpochMin(shift.startedAtEpochMin);
  const dateStr = formatShiftDate(shift.startedAtEpochMin);
  const chrono = sortAktualChronological(shift.aktual, shift.startedAtEpochMin + 240);
  const lines = chrono.map((a, i) => {
    const mesin = db[a.mcNo];
    const corak = a.corakOverride ?? mesin?.corak ?? "—";
    const yard = a.customYard ?? mesin?.targetYard;
    const suffix = yard != null ? ` · ${formatYard(yard)}y` : "";
    return `${i + 1}. Mc${a.mcNo} · ${corak}${suffix} · ${a.ket}`;
  });
  return `Bravo!!!\nShift ${shiftNo} · ${dateStr}\n\n*Selesai (${shift.aktual.length} doff)*\n${lines.join(
    "\n",
  )}\n\nTotal: ${shift.aktual.length} doff`;
}

/** Pakai Web Share API kalau tersedia (umumnya di HP), fallback salin ke clipboard. */
export async function shareOrCopy(text: string, title: string): Promise<"shared" | "copied" | "failed"> {
  if (navigator.share) {
    try {
      await navigator.share({ text, title });
      return "shared";
    } catch {
      // Pengguna membatalkan share sheet — bukan kegagalan nyata, jangan fallback ke clipboard.
      return "failed";
    }
  }
  try {
    await navigator.clipboard.writeText(text);
    return "copied";
  } catch {
    return "failed";
  }
}
