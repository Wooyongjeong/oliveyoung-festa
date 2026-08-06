import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { App } from './App'

describe('App', () => {
  it('shows the stage zero landing page', () => {
    render(<App />)
    expect(screen.getByRole('heading', { name: '페스타 티켓' })).toBeInTheDocument()
    expect(screen.getByText('서비스 준비 완료')).toBeInTheDocument()
  })
})
