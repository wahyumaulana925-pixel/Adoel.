import { useDoffStore } from '../store/useDoffStore'

export default function Aktual() {
  const { aktual, hapusAktual } = useDoffStore()

  return (
    <div style={{ padding: 16, maxWidth: 480, margin: '0 auto' }}>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 12 }}>
        Riwayat Doffing ({aktual.length})
      </h2>
      {aktual.length === 0 && <p style={{ fontSize: 13, color: '#999' }}>Belum ada catatan.</p>}
      {aktual.map((a, i) => (
        <div
          key={i}
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            padding: '10px 12px',
            border: '1px solid #eee',
            borderRadius: 8,
            marginBottom: 6,
          }}
        >
          <div>
            <div style={{ fontWeight: 600 }}>Mc {a.mcNo}</div>
            <div style={{ fontSize: 12, color: '#666' }}>
              {a.corak} · {a.keterangan}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontWeight: 600 }}>{a.jamLabel}</div>
            <button
              onClick={() => hapusAktual(i)}
              style={{ fontSize: 11, color: '#cc0000', marginTop: 4 }}
            >
              Hapus
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}
