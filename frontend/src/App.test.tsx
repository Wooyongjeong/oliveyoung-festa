import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { App } from './App'

describe('App', () => {
  afterEach(() => vi.restoreAllMocks())

  it('logs in and shows event inventory', async () => {
    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(jsonResponse({
        accessToken: 'user-id',
        tokenType: 'Bearer',
        user: { displayName: '데모 고객', role: 'CUSTOMER' },
      }))
      .mockResolvedValueOnce(jsonResponse([{
        id: 'event-id',
        name: '올리브영 페스타 2026',
        eventStartsAt: '2026-09-07T01:00:00Z',
        status: 'ON_SALE',
      }]))
      .mockResolvedValueOnce(jsonResponse({
        id: 'event-id',
        name: '올리브영 페스타 2026',
        description: '뷰티와 웰니스 브랜드를 만나는 페스타입니다.',
        eventStartsAt: '2026-09-07T01:00:00Z',
        status: 'ON_SALE',
        grades: [{ id: 'grade-id', code: 'GENERAL', name: '일반', price: 30000, currency: 'KRW', available: 1 }],
      }))

    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: '로그인' }))

    await waitFor(() => expect(screen.getByRole('heading', { name: '판매 이벤트' })).toBeInTheDocument())
    expect(screen.getByText('올리브영 페스타 2026')).toBeInTheDocument()
    expect(screen.getByText('잔여 1매')).toBeInTheDocument()
    expect(globalThis.fetch).toHaveBeenNthCalledWith(2, '/api/events', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer user-id' }),
    }))
  })

  it('shows a login error returned by the API', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(jsonResponse(
      { message: '등록된 활성 데모 사용자가 아닙니다.' },
      false,
    ))

    render(<App />)
    fireEvent.change(screen.getByLabelText('데모 이메일'), { target: { value: 'unknown@festa.local' } })
    fireEvent.click(screen.getByRole('button', { name: '로그인' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('등록된 활성 데모 사용자가 아닙니다.')
  })
})

function jsonResponse(body: unknown, ok = true): Response {
  return { ok, json: async () => body } as Response
}
