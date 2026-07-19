/** Penjelasan singkat pertama-kali — juga bisa dibuka kapan saja lewat Pengaturan > Bantuan,
 * jadi kontennya ditulis sekali di sini dan dipakai kedua pintu masuk itu. Poin-poin di bawah
 * di-port verbatim dari OnboardingDialog.kt (aplikasi Android) supaya panduannya identik. */
const BULLETS = [
  "ESTIMASI (⏱): Ketik nomor mesin di konsol bawah, ketuk ikon jam, lalu isi sisa menit (Tappet/Cam), yard berjalan (D405), atau jam counter (D408) sesuai petunjuk di layar.",
  "DOFFING / POTONG KAIN (✂): Cara cepat — geser kartu mesin di layar Radar ke kanan (Potong Normal) atau ke kiri (Potong Matching). Cara langsung — ketik nomor mesin di konsol bawah, ketuk ikon gunting, lalu pilih tindakan.",
  "URUNGKAN & ULANG (↩ / ↪): Salah mencatat atau salah hapus? Tombol Undo/Redo di kiri konsol bawah mengembalikan data secara instan.",
  "KENDALA MESIN & MACET: Jika mesin berhenti/macet (mis. putus lusi), hapus estimasinya (tekan lama kartu radar lalu pilih Hapus) agar perhitungan waktu JEDA istirahat tetap akurat.",
];

export function OnboardingDialog({ onClose }: { onClose: () => void }) {
  return (
    <div className="dialog-backdrop" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div style={{ fontWeight: 800, fontSize: 16, marginBottom: 16 }}>Cara Pakai Adoel</div>
        {BULLETS.map((b, i) => (
          <div key={i} style={{ fontSize: 13, color: "var(--text-secondary)", lineHeight: 1.5, marginBottom: 10 }}>
            •&nbsp;&nbsp;{b}
          </div>
        ))}
        <button className="btn primary full" style={{ marginTop: 10 }} onClick={onClose}>
          Mengerti
        </button>
      </div>
    </div>
  );
}
