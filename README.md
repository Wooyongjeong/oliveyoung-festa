# Olive Young Festa Ticket

선착순 페스타 티켓의 대기열, 원자적 재고 점유, Mock 결제, 비동기 티켓 발급을 구현하는 풀스택 프로젝트다.

- [전체 아키텍처](./ARCHITECTURE.md)
- [구현 계획](./IMPLEMENTATION_PLAN.md)
- [요구사항](./PROJECT_SPEC.md)

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

단계 0은 애플리케이션 골격과 인프라 연결까지만 포함한다. 이벤트·재고 스키마와 실제 사용자 기능은 다음 단계에서 추가한다.
