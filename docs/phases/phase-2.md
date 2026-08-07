# 단계 2 — 원자적 주문 생성과 재고 점유

상태: **완료**

## 목표

PostgreSQL 트랜잭션과 조건부 갱신으로 초과 판매와 사용자 중복 구매를 막는 주문·점유 기반을 구현한다.

## 체크리스트

- [x] 주문·점유·구매 권리·상태 이력 스키마와 핵심 제약
- [x] 서버 기준 가격·통화·이벤트·등급 스냅샷
- [x] 주문 상태 전이 규칙과 단위 테스트
- [x] `available > 0` 조건의 원자적 재고 점유
- [x] 사용자·이벤트별 구매 권리 원자적 선점
- [x] `Idempotency-Key`와 요청 본문 해시 처리
- [x] 주문 생성 및 내 주문 조회 API
- [x] 재고 2매/동시 요청 100건에서 성공 정확히 2건
- [x] 동일 사용자의 두 번째 활성 주문 차단
- [x] 재고 불변식과 멱등 충돌 테스트
- [x] `@LoginUser` MVC argument resolver 적용

## 구현 내용

- `@LoginUser`와 `LoginUserArgumentResolver`를 추가했다. 기존 인증 필터가 request attribute에 넣은 `AuthenticatedUser`를 Controller 인자로 주입하며, 인증되지 않은 요청은 기존 `AUTH_401` 오류로 처리한다.
- Flyway V3에 `orders`, `reservations`, `purchase_rights`, `order_status_histories`와 상태·수량·멱등성 제약을 추가했다.
- `OrderService`가 짧은 DB 트랜잭션에서 주문 스냅샷 저장, 구매 권리 선점, 조건부 재고 점유, 점유 생성과 상태 이력 기록을 수행한다.
- `POST /api/orders`는 `Idempotency-Key`를 필수로 받고 HTTP 201/`ORDER_001`을 반환한다. 동일 사용자·키·본문 요청은 기존 주문을 반환하고, 다른 본문은 `IDEMPOTENCY_409`으로 거절한다.
- `GET /api/me/orders`는 HTTP 200/`ORDER_002`로 현재 고객의 주문 스냅샷과 점유 만료 시각을 반환한다.
- `OrderStatus`에 허용 상태 전이를 명시했다. 실제 결제·만료 전이는 이후 단계에서 이 규칙을 사용한다.
- Spring Data JPA로 전환했다. `UserRepository`는 `JpaRepository`를 사용하고, 카탈로그·주문 저장소는 JPA `EntityManager`를 사용한다. 재고·구매 권리 조건부 갱신은 PostgreSQL 원자성을 위해 native query로 유지한다. Querydsl은 현재 단순 조회에 필요하지 않아 추가하지 않았다.

## 검증 결과

- `./gradlew :backend:test` 통과
- Testcontainers PostgreSQL 통합 테스트 통과: 재고 2매에서 서로 다른 사용자 100명 동시 주문 중 성공 정확히 2건
- 같은 사용자 두 번째 주문 차단, 같은 키·같은 본문 재요청의 기존 주문 반환, 다른 본문의 `IDEMPOTENCY_409` 검증
- 주문 생성 뒤 등급 가격·이름을 변경해도 저장된 주문 스냅샷이 유지되는지 검증
- 주문 상태 허용 전이 단위 테스트 및 `@LoginUser` 기반 주문 API HTTP 201 envelope 테스트 통과
- `docker compose up --build -d backend` 후 실제 PostgreSQL V3 migration, `ORDER_001` 주문 생성과 `ORDER_002` 내 주문 조회 확인
- Spring Data JPA 전환 후 `./gradlew :backend:test` 통과

## 남은 사항

- 결제, 점유 만료, Redis 대기열은 이후 단계 범위다.
