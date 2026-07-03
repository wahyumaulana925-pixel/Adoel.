import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ErrorBoundary } from '../ErrorBoundary'

describe('ErrorBoundary', () => {
  it('renders children without error', () => {
    render(
      <ErrorBoundary>
        <div>Test Content</div>
      </ErrorBoundary>
    )
    expect(screen.getByText('Test Content')).toBeDefined()
  })

  it('renders safe content', () => {
    render(
      <ErrorBoundary>
        <div>Safe Content</div>
      </ErrorBoundary>
    )
    const element = screen.getByText('Safe Content')
    expect(element).toBeDefined()
    expect(element.textContent).toBe('Safe Content')
  })
})
