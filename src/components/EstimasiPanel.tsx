import { shiftAbsKeJamStr } from '../store/useDoffStore'
import type { Estimasi } from '../types'

interface EstimasiPanelProps {
  estimasi: Record<string, Estimasi>
  onBatalClick: (mcNo: string) => void
}

export function EstimasiPanel({ estimasi, onBatalClick }: EstimasiPanelProps) {
  const daftarEstimasi = Object.values(estimasi).sort((a, b) => a.estAbsMin - b.estAbsMin)

  if (daftarEstimasi.length === 0) {
    return (
      <div style={{ padding: 16, maxWidth: 480, margin: '0 auto', textAlign: 'center' }}>
        <p style={{ fontSize: 13, color: '#999' }}>Belum ada estimasi.</p>
      </div>
    )
  }

  return (
    <div style={{ padding: '0 16px 100px', maxWidth: 480, margin: '0 auto' }}>
      <h3 style={{ fontSize: 14, fontWeight: 600, color: '#666', marginBottom: 8 }}>
        Estimasi Doffing ({daftarEstimasi.length})
      </h3>
      {daftarEstimasi.map((est) => (
        <div
          key={est.mcNo}
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '12px',
            border: '1px solid #fde047',
            borderRadius: 8,
            marginBottom: 8,
            background: '#fffbeb',
          }}
        >
          <div>
            <div style={{ fontWeight: 600 }}>Mc {est.mcNo}</div>
            <div style={{ fontSize: 12, color: '#999' }}>Est: {shiftAbsKeJamStr(est.estAbsMin)}</div>
          </div>
          <button
            onClick={() => onBatalClick(est.mcNo)}
            style={{
              padding: '6px 12px',
              background: '#fbbf24',
              color: '#92400e',
              border: 'none',
              borderRadius: 6,
              fontSize: 12,
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Batal
          </button>
        </div>
      ))}
    </div>
  )
}
