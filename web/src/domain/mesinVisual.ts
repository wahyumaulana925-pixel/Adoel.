import type { MesinTipe } from "./types";

/** Satu warna per MesinTipe — port 1:1 dari mesinTipeColor di Icons.kt (aplikasi Android),
 * dipakai di mana pun tipe mesin butuh penanda visual (RadarCard, baris Riwayat, Pengaturan >
 * Mesin, breakdown Statistik) supaya bahasa warnanya tidak melenceng antar layar. Sengaja bukan
 * salah satu dari 4 warna status (hijau/amber/merah/cyan) supaya lencana tipe mesin tidak
 * terbaca sebagai status sekilas pandang. */
export const TIPE_COLOR: Record<MesinTipe, string> = {
  TAPPET: "#14b8a6",
  CAM: "#8b5cf6",
  D405: "#6366f1",
  D408: "#d946ef",
};
