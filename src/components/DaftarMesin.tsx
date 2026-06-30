import { useState } from 'react'
import { useDoffStore } from '../store/useDoffStore'
import type { MesinTipe } from '../types'

export default function DaftarMesin() {
  const { db, setMesin } = useDoffStore()
  const [search, setSearch] = useState('')
  const [editingMc, setEditingMc] = useState<string | null>(null)

  const semuaMc = Object.keys(db)
    .map(Number)
    .sort((a, b) => a - b)
    .map(String)

  const filtered = semuaMc.filter((mc) => {
    if (!search) return true
    const mesin = db[mc]
    return mc.includes(search) || mesin.corak.toLowerCase().includes(search.toLowerCase())
  })

  const editing = editingMc ? db[editingMc] : null

  return (
    <div style={{ padding: 16, maxWidth: 480, margin: '0 auto' }}>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 10 }}>Daftar Mesin</h2>
      <input
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        placeholder="Cari nomor mesin atau corak..."
        style={{ width: '100%', padding: 10, border: '1px solid #ccc', borderRadius: 8, marginBottom: 12 }}
      />

      {filtered.map((mc) => {
        const mesin = db[mc]
        return (
          <div
            key={mc}
            onClick={() => setEditingMc(mc)}
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              padding: '10px 12px',
              borderBottom: '1px solid #eee',
            }}
          >
            <div>
              <span style={{ fontWeight: 600 }}>Mc {mc}</span>
              <span style={{ fontSize: 12, color: '#888', marginLeft: 8 }}>{mesin.tipe}</span>
            </div>
            <div style={{ color: mesin.corak === '-' ? '#bbb' : '#333' }}>{mesin.corak}</div>
          </div>
        )
      })}

      {editing && editingMc && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.4)',
            display: 'flex',
            alignItems: 'flex-end',
          }}
          onClick={() => setEditingMc(null)}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{ background: '#fff', width: '100%', padding: 20, borderRadius: '16px 16px 0 0' }}
          >
            <h3 style={{ fontWeight: 700, marginBottom: 12 }}>Edit Mc {editingMc}</h3>

            <label style={{ fontSize: 12, color: '#666' }}>Tipe Mesin</label>
            <select
              value={editing.tipe}
              onChange={(e) =>
                setMesin(editingMc, { ...editing, tipe: e.target.value as MesinTipe })
              }
              style={{ width: '100%', padding: 10, marginBottom: 10, border: '1px solid #ccc', borderRadius: 8 }}
            >
              <option value="TAPPET">TAPPET</option>
              <option value="CAM">CAM</option>
              <option value="D405">D405</option>
              <option value="D408">D408</option>
            </select>

            <label style={{ fontSize: 12, color: '#666' }}>Corak</label>
            <input
              value={editing.corak}
              onChange={(e) => setMesin(editingMc, { ...editing, corak: e.target.value })}
              style={{ width: '100%', padding: 10, marginBottom: 10, border: '1px solid #ccc', borderRadius: 8 }}
            />

            {(editing.tipe === 'TAPPET' || editing.tipe === 'CAM' || editing.tipe === 'D405') && (
              <>
                <label style={{ fontSize: 12, color: '#666' }}>Target Yard</label>
                <input
                  type="number"
                  value={editing.targetYard ?? ''}
                  onChange={(e) =>
                    setMesin(editingMc, { ...editing, targetYard: parseFloat(e.target.value) || undefined })
                  }
                  style={{ width: '100%', padding: 10, marginBottom: 10, border: '1px solid #ccc', borderRadius: 8 }}
                />
              </>
            )}

            {editing.tipe === 'D405' && (
              <>
                <label style={{ fontSize: 12, color: '#666' }}>Speed (yard/menit)</label>
                <input
                  type="number"
                  step="0.001"
                  value={editing.speed ?? ''}
                  onChange={(e) =>
                    setMesin(editingMc, { ...editing, speed: parseFloat(e.target.value) || undefined })
                  }
                  style={{ width: '100%', padding: 10, marginBottom: 10, border: '1px solid #ccc', borderRadius: 8 }}
                />
              </>
            )}

            {editing.tipe === 'D408' && (
              <>
                <label style={{ fontSize: 12, color: '#666' }}>Koreksi (menit)</label>
                <input
                  type="number"
                  value={editing.koreksi ?? ''}
                  onChange={(e) =>
                    setMesin(editingMc, { ...editing, koreksi: parseFloat(e.target.value) || undefined })
                  }
                  style={{ width: '100%', padding: 10, marginBottom: 10, border: '1px solid #ccc', borderRadius: 8 }}
                />
              </>
            )}

            <button
              onClick={() => setEditingMc(null)}
              style={{ width: '100%', padding: 12, marginTop: 8, background: '#0066ff', color: '#fff', borderRadius: 8 }}
            >
              Selesai
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
