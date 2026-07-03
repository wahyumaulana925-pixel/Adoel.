import { afterEach, vi } from 'vitest'
import { cleanup } from '@testing-library/react'

// Cleanup after each test
afterEach(() => {
  cleanup()
})

// Mock Capacitor modules
vi.mock('@capacitor/local-notifications', () => ({
  LocalNotifications: {
    checkPermissions: vi.fn(() => Promise.resolve({ display: 'granted' })),
    requestPermissions: vi.fn(() => Promise.resolve({ display: 'granted' })),
    schedule: vi.fn(() => Promise.resolve()),
    cancel: vi.fn(() => Promise.resolve()),
  },
}))
