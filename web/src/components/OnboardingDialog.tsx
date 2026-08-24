import { useState } from "react";

/** Penjelasan singkat pertama-kali — juga bisa dibuka kapan saja lewat Pengaturan > Panduan Penggunaan,
 * jadi kontennya ditulis sekali di sini dan dipakai kedua pintu masuk itu. Poin-poin di bawah
 * di-port verbatim dari OnboardingDialog.kt (aplikasi Android) supaya panduannya identik. */
const BULLETS = [
  "ESTIMASI (⏱): Ketik nomor mesin di konsol bawah, ketuk ikon jam, lalu isi sisa menit (Tappet/Cam), yard berjalan (D405), atau jam counter (D408) sesuai petunjuk di layar.",
  "DOFFING / POTONG KAIN (✂): Cara cepat — geser kartu mesin di layar Radar ke kanan (Potong Normal) atau ke kiri (Potong Matching). Cara langsung — ketik nomor mesin di konsol bawah, ketuk ikon gunting, lalu pilih tindakan.",
  "URUNGKAN & ULANG (↩ / ↪): Salah mencatat atau salah hapus? Tombol Undo/Redo di kiri konsol bawah mengembalikan data secara instan.",
  "KENDALA MESIN & MACET: Jika mesin berhenti/macet (mis. putus lusi), hapus estimasinya (tekan lama kartu radar lalu pilih Hapus) agar perhitungan waktu JEDA istirahat tetap akurat.",
];

export function OnboardingDialog({ onClose }: { onClose: () => void }) {
  const [tab, setTab] = useState<"INPUT" | "GESTURE">("INPUT");
  return (
    <div className="dialog-backdrop" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div style={{ fontWeight: 800, fontSize: 16, marginBottom: 12 }}>Panduan Penggunaan</div>
        <div className="chip-row-wrap" style={{ marginBottom: 14 }}>
          <button className={`chip-btn${tab === "INPUT" ? " active" : ""}`} onClick={() => setTab("INPUT")}>Input Data Mesin</button>
          <button className={`chip-btn${tab === "GESTURE" ? " active" : ""}`} onClick={() => setTab("GESTURE")}>Simulasi 5 Gestur</button>
        </div>
        {tab === "INPUT" && BULLETS.map((b, i) => (
          <div key={i} style={{ fontSize: 13, color: "var(--text-secondary)", lineHeight: 1.5, marginBottom: 10 }}>
            •&nbsp;&nbsp;{b}
          </div>
        ))}
        {tab === "GESTURE" && ["Geser kanan: doffing normal", "Geser kiri: doffing matching", "Tekan lama: buka aksi kartu", "Ketuk waktu: edit estimasi", "Ketuk nomor/corak: edit data mesin"].map((gesture) => (
          <div key={gesture} style={{ fontSize: 13, color: "var(--text-secondary)", lineHeight: 1.5, marginBottom: 10 }}>•&nbsp;&nbsp;{gesture}</div>
        ))}
        <button className="btn primary full" style={{ marginTop: 10 }} onClick={onClose}>
          Mengerti
        </button>
      </div>
    </div>
  );
}
