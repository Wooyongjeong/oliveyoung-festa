# 단계 1 — 이벤트 카탈로그와 개발용 인증

상태: **완료**

## 목표

데모 사용자가 로그인하고 판매 이벤트, 티켓 등급, 가격과 잔여 재고를 조회할 수 있게 한다.

## 체크리스트

- [x] `users`, `events`, `ticket_grades`, `inventories` 스키마와 제약
- [x] 고객·운영자·관리자 데모 사용자 seed
- [x] 판매 중 데모 이벤트와 GENERAL/VIP 재고 seed
- [x] 개발용 로그인과 Bearer 인증
- [x] `CUSTOMER`, `OPERATOR`, `ADMIN` 역할 처리
- [x] 이벤트 목록 API
- [x] 이벤트 상세 및 등급별 잔여 수량 API
- [x] 판매 전·판매 중·판매 종료 상태 판정 테스트
- [x] 비로그인·권한 오류의 표준 응답 테스트
- [x] 로그인과 이벤트 재고를 표시하는 최소 UI
- [x] 실행 및 데모 계정 문서화

## 구현 내용

- Flyway V2에 네 개의 업무 테이블, 수량·통화·기간 제약과 고정 UUID seed를 추가했다.
- `POST /api/auth/dev-login`, `GET /api/auth/me`를 구현했다. 개발 토큰은 활성 사용자의 UUID이며 매 요청 DB에서 사용자를 확인한다.
- `GET /api/events`, `GET /api/events/{eventId}`를 구현했다. 이벤트 상태 판정에는 조회 시 PostgreSQL `CURRENT_TIMESTAMP`를 사용한다.
- 인증·권한·검증·이벤트 없음 오류를 공통 `ApiError` 형식으로 반환한다.
- 프런트에서 데모 로그인 후 이벤트 상태, 가격과 등급별 잔여 수량을 표시한다.
- 개발 인증은 로컬 시연 전용이며 서명된 운영 토큰이 아니라는 제한을 README에 기록했다.

## 검증 결과

- `./gradlew :backend:test` 통과
- `./gradlew test` 통과
- `cd frontend && npm test` 통과: 2개 테스트
- `cd frontend && npm run build` 통과
- `docker compose up --build -d` 성공 및 모든 서비스 healthy
- 실제 PostgreSQL에서 Flyway V2와 seed 적용 확인
- 로그인 → 이벤트 목록 → 이벤트 상세 API에서 GENERAL/VIP 각 1매 조회 확인

## 남은 사항

- 주문, 점유와 구매 제한은 단계 2에서 구현한다.
