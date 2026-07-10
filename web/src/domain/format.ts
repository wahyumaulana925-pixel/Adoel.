// Port 1:1 dari data/Models.kt (aplikasi Android Adoel) — jaga semua rumus & format
// tetap identik supaya backup JSON dari HP tetap kompatibel dan hasil hitungan sama.

/** Epoch minute sekarang (Unix ms / 60000, dibulatkan bawah). Field waktu di semua
 * tipe data (estAbsMin, tsEpochMin, dst) memakai satuan ini, BUKAN detik/milidetik. */
export function nowAbsMin(): number {
  return Math.floor(Date.now() / 60000);
}

export function absMinToTimeStr(absMin: number): string {
  const d = new Date(absMin * 60000);
  return `${pad2(d.getHours())}.${pad2(d.getMinutes())}`;
}

export function nowTimeStr(): string {
  const d = new Date();
  return `${pad2(d.getHours())}.${pad2(d.getMinutes())}`;
}

function pad2(n: number): string {
  return n.toString().padStart(2, "0");
}

export function formatDeltaMin(deltaMin: number): string {
  const sign = deltaMin < 0 ? "−" : "";
  const mag = Math.abs(deltaMin);
  return mag >= 60 ? `${sign}${Math.floor(mag / 60)}j${mag % 60}m` : `${sign}${mag}m`;
}

export function formatYard(y: number): string {
  return Number.isInteger(y) ? String(y) : String(y);
}

/** Jadwal 3 shift tetap: Shift 1 06.00-14.00, Shift 2 14.00-22.00, Shift 3 22.00-06.00
 * (lintas tengah malam). Diklasifikasi dari jam mulai shift itu. */
export function shiftNumberForEpochMin(epochMin: number): 1 | 2 | 3 {
  const hour = new Date(epochMin * 60000).getHours();
  if (hour >= 6 && hour < 14) return 1;
  if (hour >= 14 && hour < 22) return 2;
  return 3;
}

/** Epoch-minute mulainya periode shift SAAT INI (batas 06.00/14.00/22.00 terdekat
 * sebelum/pas epochMin) - dipakai untuk mendeteksi entri tertinggal dari shift
 * sebelumnya yang belum diarsipkan lewat "Selesai Shift". */
export function currentShiftStartAbsMin(epochMin: number): number {
  const d = new Date(epochMin * 60000);
  const hour = d.getHours();
  const boundaryHour = hour >= 6 && hour < 22 ? (hour < 14 ? 6 : 14) : 22;
  if (hour < 6) d.setDate(d.getDate() - 1);
  d.setHours(boundaryHour, 0, 0, 0);
  return Math.floor(d.getTime() / 60000);
}

/** Dipakai khusus tipe mesin D408: mengubah "bacaan jam pada counter mesin" (bukan
 * jam sekarang) jadi waktu absolut TERDEKAT dari sekarang - kalau selisihnya lebih
 * dari 12 jam, geser +-1 hari supaya jadi yang paling masuk akal. */
export function jamKeShiftAbs(jamMin: number): number {
  const startOfDay = new Date();
  startOfDay.setHours(0, 0, 0, 0);
  const epochMinToday = Math.floor(startOfDay.getTime() / 60000) + jamMin;
  const currentEpochMin = nowAbsMin();
  const diff = epochMinToday - currentEpochMin;
  if (diff < -720) return epochMinToday + 1440;
  if (diff > 720) return epochMinToday - 1440;
  return epochMinToday;
}
