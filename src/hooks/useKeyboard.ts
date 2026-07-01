import { useState, useEffect } from 'react'

/**
 * Returns the estimated on-screen keyboard height in pixels.
 * 0 when keyboard is closed. Uses Visual Viewport API so it works
 * regardless of windowSoftInputMode (adjustResize or adjustNothing).
 */
export function useKeyboard(): number {
  const [kbH, setKbH] = useState(0)

  useEffect(() => {
    const vv = window.visualViewport
    if (!vv) return
    const update = () => {
      const diff = window.innerHeight - vv.height
      setKbH(diff > 80 ? Math.round(diff) : 0)
    }
    vv.addEventListener('resize', update)
    return () => vv.removeEventListener('resize', update)
  }, [])

  return kbH
}
