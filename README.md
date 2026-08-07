# Olive Young Festa Ticket

선착순 페스타 티켓의 대기열, 원자적 재고 점유, Mock 결제, 비동기 티켓 발급을 구현하는 풀스택 프로젝트다.

- [문서 인덱스](./docs/README.md)
- [전체 아키텍처](./docs/ARCHITECTURE.md)
- [구현 계획](./docs/IMPLEMENTATION_PLAN.md)
- [요구사항](./docs/PROJECT_SPEC.md)
- [단계별 진행 현황](./docs/phases/README.md)

## 요구 환경

- Docker와 Docker Compose
- 로컬 개발 시 Java 21+, Node.js 22+

## 전체 서비스 실행

```bash
cp .env.example .env
docker compose up --build
```

실행 후 다음 주소에서 상태를 확인한다.

- Frontend: <http://localhost:5173>
- Backend health: <http://localhost:8080/actuator/health>
- Mock PG health: <http://localhost:8081/actuator/health>

## 테스트

```bash
./gradlew test
cd frontend && npm ci && npm test
```

## 단계 1 데모 흐름

프런트에서 `customer@festa.local`로 로그인하면 판매 이벤트와 GENERAL/VIP 가격·잔여 수량을 확인할 수 있다. 개발용 사용자는 다음과 같다.

| 이메일 | 역할 |
|---|---|
| `customer@festa.local` | `CUSTOMER` |
| `operator@festa.local` | `OPERATOR` |
| `admin@festa.local` | `ADMIN` |

개발 로그인 API는 사용자 UUID를 Bearer 토큰으로 반환한다. 비밀번호나 토큰 서명이 없는 로컬 개발·시연 전용 방식이며 운영 환경에 사용하지 않는다.

```bash
curl -s http://localhost:8080/api/auth/dev-login \
  -H 'Content-Type: application/json' \
  -d '{"email":"customer@festa.local"}'

curl -s http://localhost:8080/api/events \
  -H 'Authorization: Bearer 10000000-0000-0000-0000-000000000001'
```

Flyway seed는 판매 중인 데모 이벤트 1개와 GENERAL/VIP 각 1매를 생성한다. 판매 상태 판정에는 PostgreSQL `CURRENT_TIMESTAMP`를 사용한다.
