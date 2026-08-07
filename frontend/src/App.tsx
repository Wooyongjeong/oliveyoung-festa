import { FormEvent, useState } from 'react'

type User = {
  displayName: string
  role: 'CUSTOMER' | 'OPERATOR' | 'ADMIN'
}

type LoginResponse = {
  accessToken: string
  tokenType: string
  user: User
}

type ApiResponse<T> = {
  statusCode: string
  statusMessage: string
  body: T
}

type EventSummary = {
  id: string
  name: string
  eventStartsAt: string
  status: 'UPCOMING' | 'ON_SALE' | 'ENDED'
}

type TicketGrade = {
  id: string
  code: string
  name: string
  price: number
  currency: string
  available: number
}

type EventDetail = EventSummary & {
  description: string
  grades: TicketGrade[]
}

type Order = {
  id: string
  eventName: string
  gradeName: string
  unitPrice: number
  status: string
}

type Payment = {
  orderId: string
  status: 'PROCESSING' | 'APPROVED' | 'DECLINED' | 'UNKNOWN' | 'REVIEW_REQUIRED'
}

const statusLabels = {
  UPCOMING: '판매 예정',
  ON_SALE: '판매 중',
  ENDED: '판매 종료',
}

export function App() {
  const [email, setEmail] = useState('customer@festa.local')
  const [user, setUser] = useState<User | null>(null)
  const [accessToken, setAccessToken] = useState('')
  const [events, setEvents] = useState<EventDetail[]>([])
  const [orders, setOrders] = useState<Order[]>([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function login(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setError('')

    try {
      const loginResponse = await request<LoginResponse>('/api/auth/dev-login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      })
      const eventSummaries = await request<EventSummary[]>('/api/events', {}, loginResponse.accessToken)
      const eventDetails = await Promise.all(
        eventSummaries.map((summary) => request<EventDetail>(`/api/events/${summary.id}`, {}, loginResponse.accessToken)),
      )
      setUser(loginResponse.user)
      setAccessToken(loginResponse.accessToken)
      setEvents(eventDetails)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '요청을 처리하지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  async function purchase(eventId: string, ticketGradeCode: string) {
    setLoading(true)
    setError('')
    try {
      const order = await request<Order>('/api/orders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Idempotency-Key': crypto.randomUUID() },
        body: JSON.stringify({ eventId, ticketGradeCode }),
      }, accessToken)
      setOrders((current) => [order, ...current])
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '주문을 만들지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  async function pay(order: Order) {
    setLoading(true)
    setError('')
    try {
      const payment = await request<Payment>(`/api/orders/${order.id}/payments`, {
        method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() },
      }, accessToken)
      setOrders((current) => current.map((item) => item.id === order.id
        ? { ...item, status: payment.status === 'APPROVED' ? 'PAID' : payment.status === 'DECLINED' ? 'HELD' : payment.status }
        : item))
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '결제를 처리하지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  async function reconcile(order: Order) {
    setLoading(true)
    setError('')
    try {
      const payment = await request<Payment>(`/api/orders/${order.id}/payments/reconcile`, { method: 'POST' }, accessToken)
      setOrders((current) => current.map((item) => item.id === order.id
        ? { ...item, status: payment.status === 'APPROVED' ? 'PAID' : payment.status }
        : item))
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '결제 상태를 확인하지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  if (!user) {
    return (
      <main>
        <p className="eyebrow">OLIVE YOUNG FESTA</p>
        <h1>페스타 티켓</h1>
        <p>데모 사용자로 로그인해 판매 이벤트와 잔여 티켓을 확인하세요.</p>
        <form onSubmit={login}>
          <label htmlFor="email">데모 이메일</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
          <button type="submit" disabled={loading}>{loading ? '불러오는 중…' : '로그인'}</button>
        </form>
        {error && <p className="error" role="alert">{error}</p>}
        <p className="hint">customer@festa.local</p>
      </main>
    )
  }

  return (
    <main>
      <header>
        <div>
          <p className="eyebrow">OLIVE YOUNG FESTA</p>
          <h1>판매 이벤트</h1>
        </div>
        <span className="user">{user.displayName} · {user.role}</span>
      </header>
      <section aria-label="이벤트 목록">
        {events.map((event) => (
          <article key={event.id}>
            <div className="event-heading">
              <h2>{event.name}</h2>
              <span className={`status status-${event.status.toLowerCase()}`}>{statusLabels[event.status]}</span>
            </div>
            <p>{event.description}</p>
            <p className="date">행사일 {new Date(event.eventStartsAt).toLocaleString('ko-KR')}</p>
            <div className="grades">
              {event.grades.map((grade) => (
                <div className="grade" key={grade.id}>
                  <strong>{grade.name}</strong>
                  <span>{grade.price.toLocaleString('ko-KR')}원</span>
                  <span className="available">잔여 {grade.available}매</span>
                  <button type="button" disabled={loading || grade.available < 1}
                    onClick={() => purchase(event.id, grade.code)}>주문하기</button>
                </div>
              ))}
            </div>
          </article>
        ))}
      </section>
      {orders.length > 0 && <section aria-label="내 주문">
        <h2>내 주문</h2>
        {orders.map((order) => <article key={order.id}>
          <strong>{order.eventName} · {order.gradeName}</strong>
          <p>{order.unitPrice.toLocaleString('ko-KR')}원 · {order.status}</p>
          {order.status === 'HELD' && <button type="button" disabled={loading} onClick={() => pay(order)}>결제하기</button>}
          {(order.status === 'UNKNOWN' || order.status === 'REVIEW_REQUIRED') &&
            <button type="button" disabled={loading} onClick={() => reconcile(order)}>결제 확인</button>}
        </article>)}
      </section>}
      {error && <p className="error" role="alert">{error}</p>}
    </main>
  )
}

async function request<T>(url: string, init: RequestInit = {}, accessToken?: string): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: {
      ...init.headers,
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
  })
  const responseBody = await response.json()
  if (!response.ok) {
    throw new Error(responseBody.message ?? '요청을 처리하지 못했습니다.')
  }
  return (responseBody as ApiResponse<T>).body
}
