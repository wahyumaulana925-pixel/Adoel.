import type { MesinData } from "./types";

/** Database awal: 174 mesin (nomor 1-174), semuanya kosong (tipe TAPPET, corak "-")
 * — sengaja tidak diisi preset pabrik tertentu di sini karena itu data spesifik
 * pabrik. Cara mengisi data asli: atur satu-satu di Pengaturan, atau paling cepat
 * import file backup JSON hasil "Cadangkan" dari aplikasi Android. */
export function buildDefaultDb(): Record<string, MesinData> {
  const db: Record<string, MesinData> = {};
  for (let i = 1; i <= 174; i++) {
    db[String(i)] = { tipe: "TAPPET", corak: "-", targetYard: null, speed: null, koreksi: null };
  }
  return db;
}
