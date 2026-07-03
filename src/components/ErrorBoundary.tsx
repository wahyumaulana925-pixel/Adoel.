import React, { type ReactNode } from 'react'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
}

export class ErrorBoundary extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div
          style={{
            padding: 20,
            maxWidth: 480,
            margin: '0 auto',
            textAlign: 'center',
            fontFamily: 'sans-serif',
          }}
        >
          <h1 style={{ fontSize: 24, fontWeight: 700, color: '#cc0000', marginBottom: 8 }}>
            Oops! Ada kesalahan
          </h1>
          <p style={{ fontSize: 14, color: '#666', marginBottom: 16 }}>
            Aplikasi mengalami masalah yang tidak terduga.
          </p>
          <p style={{ fontSize: 12, color: '#999', marginBottom: 20, fontFamily: 'monospace' }}>
            {this.state.error?.message || 'Unknown error'}
          </p>
          <button
            onClick={() => {
              this.setState({ hasError: false, error: null })
              window.location.reload()
            }}
            style={{
              padding: '10px 20px',
              background: '#0066ff',
              color: '#fff',
              border: 'none',
              borderRadius: 8,
              cursor: 'pointer',
              fontWeight: 600,
            }}
          >
            Muat Ulang Aplikasi
          </button>
        </div>
      )
    }

    return this.props.children
  }
}
