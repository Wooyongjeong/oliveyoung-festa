import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { App } from './App'

describe('App', () => {
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('logs in and shows event inventory', async () => {
    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(successResponse('AUTH_001', '로그인 성공', {
        accessToken: 'user-id',
        tokenType: 'Bearer',
        user: { displayName: '데모 고객', role: 'CUSTOMER' },
      }))
      .mockResolvedValueOnce(successResponse('EVENT_001', '이벤트 목록 조회 성공', [{
          id: 'event-id',
          name: '올리브영 페스타 2026',
          eventStartsAt: '2026-09-07T01:00:00Z',
          status: 'ON_SALE',
        }]))
      .mockResolvedValueOnce(successResponse('EVENT_002', '이벤트 상세 조회 성공', {
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

  it('creates and pays for an order', async () => {
    vi.spyOn(globalThis.crypto, 'randomUUID').mockReturnValue('00000000-0000-4000-8000-000000000001')
    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(successResponse('AUTH_001', '로그인 성공', {
        accessToken: 'user-id', tokenType: 'Bearer', user: { displayName: '데모 고객', role: 'CUSTOMER' },
      }))
      .mockResolvedValueOnce(successResponse('EVENT_001', '조회 성공', [{
        id: 'event-id', name: '페스타', eventStartsAt: '2026-09-07T01:00:00Z', status: 'ON_SALE',
      }]))
      .mockResolvedValueOnce(successResponse('EVENT_002', '조회 성공', {
        id: 'event-id', name: '페스타', description: '테스트', eventStartsAt: '2026-09-07T01:00:00Z', status: 'ON_SALE',
        grades: [{ id: 'grade-id', code: 'GENERAL', name: '일반', price: 30000, currency: 'KRW', available: 1 }],
      }))
      .mockResolvedValueOnce(successResponse('ORDER_001', '주문 생성 성공', {
        id: 'order-id', eventName: '페스타', gradeName: '일반', unitPrice: 30000, status: 'HELD',
      }))
      .mockResolvedValueOnce(successResponse('PAYMENT_001', '결제 처리 성공', {
        orderId: 'order-id', status: 'APPROVED',
      }))

    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: '로그인' }))
    fireEvent.click(await screen.findByRole('button', { name: '주문하기' }))
    fireEvent.click(await screen.findByRole('button', { name: '결제하기' }))

    expect(await screen.findByText('30,000원 · PAID')).toBeInTheDocument()
  })
})

function jsonResponse(body: unknown, ok = true): Response {
  return { ok, json: async () => body } as Response
}

function successResponse(statusCode: string, statusMessage: string, body: unknown): Response {
  return jsonResponse({ statusCode, statusMessage, body })
}
