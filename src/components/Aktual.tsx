import { useState } from 'react'
import { useDoffStore, shiftAbsKeJamStr, jamSekarangAbs, standarisasiKeterangan } from '../store/useDoffStore'

const KETERANGAN_OPTIONS = ['HB', 'P. LP', 'P. SN', 'P. OH', 'P. EL', 'P. Selvedge']

export default function Aktual() {
  const { db, aktual, tambahAktual, hapusAktual, hapusEstimasi } = useDoffStore()
  const [mcNo, setMcNo] = useState('')
  const [keterangan, setKeterangan] = useState('HB')

  const mesin = mcNo ? db[mcNo] : null

  const catatAktual = () => {
    if (!mcNo || !mesin) return
    const now = jamSekarangAbs()
    tambahAktual({
      mcNo,
      corak: mesin.corak,
      keterangan: standarisasiKeterangan(keterangan),
      jamLabel: shiftAbsKeJamStr(now),
      timestamp: Date.now(),
    })
    hapusEstimasi(mcNo)
    setMcNo('')
  }

  return (
    <div style={{ padding: 16, maxWidth: 480, margin: '0 auto' }}>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 10 }}>Catat Aktual</h2>

      <label style={{ fontSize: 12, color: '#666' }}>Nomor Mesin</label>
      <input
        value={mcNo}
        onChange={(e) => setMcNo(e.target.value.replace(/\D/g, ''))}
        placeholder="Mis. 49"
        style={{ width: '100%', padding: 10, marginBottom: 8, border: '1px solid #ccc', borderRadius: 8 }}
      />
      {mesin && (
        <p style={{ fontSize: 12, color: '#888', marginBottom: 8 }}>
          {mesin.tipe} · corak {mesin.corak}
        </p>
      )}
      {mcNo && !mesin && (
        <p style={{ fontSize: 12, color: '#cc0000', marginBottom: 8 }}>Mesin tidak ditemukan</p>
      )}

      <label style={{ fontSize: 12, color: '#666' }}>Keterangan</label>
      <select
        value={keterangan}
        onChange={(e) => setKeterangan(e.target.value)}
        style={{ width: '100%', padding: 10, marginBottom: 10, border: '1px solid #ccc', borderRadius: 8 }}
      >
        {KETERANGAN_OPTIONS.map((k) => (
          <option key={k} value={k}>
            {k}
          </option>
        ))}
      </select>

      <button
        onClick={catatAktual}
        disabled={!mesin}
        style={{
          width: '100%',
          padding: 12,
          background: mesin ? '#0066ff' : '#ccc',
          color: '#fff',
          borderRadius: 8,
          marginBottom: 20,
        }}
      >
        Simpan Catatan
      </button>

      <h3 style={{ fontSize: 15, fontWeight: 600, marginBottom: 8 }}>
        Riwayat Shift Ini ({aktual.length})
      </h3>
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
