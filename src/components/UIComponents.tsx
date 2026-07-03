import { useRef } from 'react'

type Mode = 'aktual' | 'estimasi'

interface ModeToggleProps {
  mode: Mode
  onModeChange: (mode: Mode) => void
  permGranted: boolean
  onRequestPerms: () => void
}

export function ModeToggle({ mode, onModeChange, permGranted, onRequestPerms }: ModeToggleProps) {
  return (
    <div style={{ padding: 16, maxWidth: 480, margin: '0 auto' }}>
      <h1 style={{ fontSize: 20, fontWeight: 700, marginBottom: 4 }}>Adoel V5</h1>
      <p style={{ fontSize: 13, color: '#666', marginBottom: 12 }}>Aplikasi Doffing Estimasi</p>

      {!permGranted && (
        <button
          onClick={onRequestPerms}
          style={{
            width: '100%',
            padding: 10,
            marginBottom: 12,
            background: '#222',
            color: '#fff',
            borderRadius: 8,
            fontWeight: 600,
            border: 'none',
            cursor: 'pointer',
          }}
        >
          Aktifkan Izin Notifikasi
        </button>
      )}

      <div
        style={{
          position: 'relative',
          display: 'flex',
          background: '#eee',
          borderRadius: 10,
          padding: 4,
          marginBottom: 8,
        }}
      >
        <button
          onClick={() => onModeChange('aktual')}
          style={{
            flex: 1,
            padding: 10,
            borderRadius: 8,
            fontWeight: 700,
            background: mode === 'aktual' ? '#06b6d4' : 'transparent',
            color: mode === 'aktual' ? '#fff' : '#666',
            transition: 'all .2s',
            border: 'none',
            cursor: 'pointer',
          }}
        >
          Aktual
        </button>
        <button
          onClick={() => onModeChange('estimasi')}
          style={{
            flex: 1,
            padding: 10,
            borderRadius: 8,
            fontWeight: 700,
            background: mode === 'estimasi' ? '#f59e0b' : 'transparent',
            color: mode === 'estimasi' ? '#fff' : '#666',
            transition: 'all .2s',
            border: 'none',
            cursor: 'pointer',
          }}
        >
          Estimasi
        </button>
      </div>
    </div>
  )
}

interface KomandInputProps {
  mode: Mode
  cmd: string
  onCmdChange: (val: string) => void
  onSubmit: () => void
}

export function KomandInput({ mode, cmd, onCmdChange, onSubmit }: KomandInputProps) {
  const inputRef = useRef<HTMLInputElement>(null)

  return (
    <div style={{ padding: '0 16px 16px', maxWidth: 480, margin: '0 auto' }}>
      <div style={{ display: 'flex', gap: 8 }}>
        <input
          ref={inputRef}
          value={cmd}
          onChange={(e) => onCmdChange(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              onSubmit()
              inputRef.current?.focus()
            }
          }}
          placeholder={mode === 'aktual' ? 'Ketik aktual (31 hb)...' : 'Ketik estimasi (61 150y)...'}
          style={{
            flex: 1,
            padding: 10,
            border: '1px solid #ccc',
            borderRadius: 8,
            fontSize: 14,
          }}
        />
        <button
          onClick={() => {
            onSubmit()
            inputRef.current?.focus()
          }}
          style={{
            padding: '10px 16px',
            background: '#0066ff',
            color: '#fff',
            border: 'none',
            borderRadius: 8,
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          Kirim
        </button>
      </div>
    </div>
  )
}

interface ToastProps {
  msg: string
  ok: boolean
}

export function Toast({ msg, ok }: ToastProps) {
  return (
    <div
      style={{
        position: 'fixed',
        bottom: 16,
        left: '50%',
        transform: 'translateX(-50%)',
        padding: '12px 20px',
        background: ok ? '#10b981' : '#ef4444',
        color: '#fff',
        borderRadius: 8,
        fontSize: 14,
        fontWeight: 500,
        maxWidth: 'calc(100% - 32px)',
      }}
    >
      {msg}
    </div>
  )
}
