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

const statusLabels = {
  UPCOMING: '판매 예정',
  ON_SALE: '판매 중',
  ENDED: '판매 종료',
}

export function App() {
  const [email, setEmail] = useState('customer@festa.local')
  const [user, setUser] = useState<User | null>(null)
  const [events, setEvents] = useState<EventDetail[]>([])
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
      setEvents(eventDetails)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '요청을 처리하지 못했습니다.')
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
                </div>
              ))}
            </div>
          </article>
        ))}
      </section>
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
