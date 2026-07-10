import type { Estimasi } from "./types";

export type UrgencyLevel = "CALM" | "SOON" | "IMMINENT" | "OVERDUE";

export function urgencyLevel(remainingMin: number): UrgencyLevel {
  if (remainingMin > 30) return "CALM";
  if (remainingMin > 10) return "SOON";
  if (remainingMin > 0) return "IMMINENT";
  return "OVERDUE";
}

export function sortedByNearest(estimasi: Record<string, Estimasi>): Estimasi[] {
  return Object.values(estimasi).sort((a, b) => a.estAbsMin - b.estAbsMin);
}

export function partitionSegeraMenunggu(sorted: Estimasi[], nowAbs: number): [Estimasi[], Estimasi[]] {
  const segera: Estimasi[] = [];
  const menunggu: Estimasi[] = [];
  for (const e of sorted) {
    if (e.estAbsMin - nowAbs <= 0) segera.push(e);
    else menunggu.push(e);
  }
  return [segera, menunggu];
}

/** Mesin tunggal yang paling butuh perhatian sekarang: paling lama overdue, kalau
 * tidak ada yang overdue baru yang paling dekat waktunya. */
export function nearestUpcoming(estimasi: Record<string, Estimasi>, nowAbs: number): Estimasi | null {
  const [segera, menunggu] = partitionSegeraMenunggu(sortedByNearest(estimasi), nowAbs);
  return segera[0] ?? menunggu[0] ?? null;
}

/** Jeda minimum (menit) antar-dua estimasi berurutan di daftar "Menunggu" supaya
 * dianggap "boleh istirahat" — sama seperti BREAK_GAP_THRESHOLD_MIN di Android. */
export const BREAK_GAP_THRESHOLD_MIN = 30;
