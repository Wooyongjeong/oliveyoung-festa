# 프로젝트 문서

## 기준 문서

- [요구사항](./PROJECT_SPEC.md)
- [구현 계획](./IMPLEMENTATION_PLAN.md)
- [아키텍처](./ARCHITECTURE.md)
- [세션 인계](./SESSION_HANDOFF.md)

## 단계별 진행

- [단계 현황과 작업 규칙](./phases/README.md)
- [단계 0 — 프로젝트 골격과 실행 환경](./phases/phase-0.md)
- [단계 1 — 이벤트 카탈로그와 개발용 인증](./phases/phase-1.md)
- [단계 2 — 원자적 주문 생성과 재고 점유](./phases/phase-2.md)
- [단계 3 — Mock 결제, 실패, 재시도와 조정](./phases/phase-3.md)
- [단계 4 — 점유 만료와 다중 인스턴스 경합](./phases/phase-4.md)
- [단계 5 — Redis 대기열과 구매 접근 제어](./phases/phase-5.md)
- [단계 6 — Kafka 기반 티켓 발급과 1회 입장](./phases/phase-6.md)
- [단계 7 — 환불, 이력과 관리자 기능](./phases/phase-7.md)
- [단계 8 — E2E, 부하 증거와 제출 마감](./phases/phase-8.md)

새 세션에서는 루트의 [AGENTS.md](../AGENTS.md)와 현재 단계 문서를 먼저 읽는다. 구현이 끝날 때마다 해당 단계의 체크리스트, 구현 내용, 검증 결과와 단계 현황을 함께 갱신한다.
