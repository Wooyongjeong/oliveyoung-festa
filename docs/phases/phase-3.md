# 단계 3 — Mock 결제, 실패, 재시도와 조정

상태: **완료**

## 목표

외부 Mock PG를 동기 HTTP로 호출하고 성공·실패·응답 유실에도 결제와 재고 상태를 정확히 확정한다.

## 체크리스트

- [x] 공통 생성·수정 시각을 관리하는 `BaseEntity`와 JPA Auditing
- [x] `PaymentGateway` 경계와 HTTP 어댑터
- [x] 독립 Mock PG 승인·조회·환불 API와 영속 상태
- [x] 결제 시도 스키마, 활성 시도 제한과 PG 고정 라우팅
- [x] PG 호출 전후 트랜잭션 분리
- [x] 결제 성공 시 판매 수량 반영과 `OrderPaid` Outbox 기록
- [x] 명확한 실패 처리와 재시도 제한
- [x] 타임아웃·응답 유실의 `UNKNOWN` 처리와 reconciliation
- [x] 결정적 지연·실패 시나리오
- [x] 결제 처리·실패·재시도 최소 UI
- [x] 성공·실패·응답 유실·중복 요청 통합 테스트

## 구현 내용

- `BaseEntity`에 `createdAt`, `lastModifiedAt`을 정의하고 JPA Auditing으로 자동 기록한다.
- `UserEntity`, `EventEntity`, `TicketGradeEntity`가 `BaseEntity`를 상속한다.
- Lombok으로 엔티티의 getter와 JPA 기본 생성자를 생성한다.
- 기존 테이블과 감사 필드 매핑을 맞추는 Flyway V4 마이그레이션을 추가했다.
- `PaymentGateway`와 `RestClient` 기반 Mock PG 어댑터를 추가하고 연결·응답 제한 시간을 적용했다.
- 독립 Mock PG에 H2 영속 승인·조회·환불 API와 성공·거절·성공 후 응답 지연 시나리오를 구현했다.
- Flyway V5에 결제 시도, 주문별 활성 시도 제한, PG·라우팅 버전, `OrderPaid` Outbox 스키마를 추가했다.
- 결제 준비와 결과 반영을 별도 트랜잭션으로 나누고 외부 HTTP 호출을 트랜잭션 밖에서 수행한다.
- 승인 시 주문·점유·재고·구매 권리·상태 이력·Outbox를 한 트랜잭션에서 반영한다.
- 거절은 2초 간격으로 3회까지 허용하며 마지막 실패에서 재고와 구매 권리를 해제한다.
- 타임아웃은 `UNKNOWN`으로 보류하고 수동 API와 5초 주기 작업으로 PG를 재조회한다. 60초 미확정 건은 `REVIEW_REQUIRED`로 전환한다.
- 프런트엔드에 주문, 결제, 실패 재시도, 불명확 결제 확인 UI를 추가했다.

## 검증 결과

- `./gradlew test` 성공: 백엔드와 Mock PG 단위·통합 테스트 통과.
- `cd frontend && npm test -- --run` 성공: 3개 UI 테스트 통과.
- `cd frontend && npm run build` 성공.

## 남은 사항

없음. Outbox Kafka 발행과 티켓 발급은 후속 단계에서 구현한다.
