// Urutan entri aktual berdasarkan jam ASLI tiap entri (bisa dikoreksi manual lewat Edit
// Riwayat), bukan urutan input ke aplikasi. Dulu list Riwayat/Statistik/teks bagikan cuma
// membalik array aktual (terbaru di-prepend di indeks 0), yang diam-diam mengasumsikan
// operator selalu mencatat doff persis sesuai urutan kejadiannya — jebol begitu operator
// mengecek riwayat potong di mesin lalu input belakangan sambil mengoreksi jam supaya sesuai
// kronologi sebenarnya (mis. gantian rekan saat istirahat): entrinya tetap nyangkut di
// posisi KAPAN DIKETIK, bukan pindah ke posisi jam yang sudah dikoreksi.
import { jamNearAbsMin } from "./format";
import { parseJam } from "./parse";
import type { AktualEntry } from "./types";

/** [anchorAbsMin] menentukan hari mana yang dimaksud sebuah jam (karena "jam" cuma string
 * "14.30" tanpa info tanggal) — pakai waktu sekarang untuk shift yang sedang berjalan, atau
 * titik tetap seperti mulainya shift untuk shift yang sudah diarsipkan (supaya tidak salah
 * hari kalau dibuka berhari-hari kemudian). Entri dengan jam tidak valid didorong ke akhir. */
export function sortAktualChronological<T extends AktualEntry>(aktual: T[], anchorAbsMin: number): T[] {
  const key = (entry: T): number => {
    const parsed = parseJam(entry.jam);
    return parsed === null ? Number.MAX_SAFE_INTEGER : jamNearAbsMin(parsed, anchorAbsMin);
  };
  return [...aktual].sort((a, b) => key(a) - key(b));
}
