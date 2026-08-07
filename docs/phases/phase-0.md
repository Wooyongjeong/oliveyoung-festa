# 단계 0 — 프로젝트 골격과 실행 환경

상태: **완료**

## 목표

백엔드, Mock PG, 프런트엔드와 PostgreSQL·Redis·Kafka를 한 저장소에서 빌드하고 Compose로 실행할 수 있는 최소 골격을 만든다.

## 체크리스트

- [x] Java 21/Spring Boot 멀티 프로젝트와 Gradle Wrapper 구성
- [x] 백엔드와 독립 Mock PG 애플리케이션 구성
- [x] React/TypeScript/Vite 프런트엔드 구성
- [x] PostgreSQL, Redis, Kafka를 포함한 `compose.yaml` 구성
- [x] Dockerfile과 서비스 헬스 체크 구성
- [x] Flyway baseline 도입
- [x] 요청 ID와 표준 API 오류 형식 구성
- [x] CI 기본 테스트 구성
- [x] 로컬 실행 및 테스트 절차 문서화

## 구현 내용

- 루트 Gradle 설정 아래 `backend`, `mock-pg` 모듈을 구성했다.
- `frontend`에 Vite 기반 React 시작 화면과 테스트 환경을 구성했다.
- `compose.yaml`에서 전체 서비스의 의존성과 readiness 헬스 체크를 연결했다.
- 백엔드에 `RequestIdFilter`, `ApiError`, `GlobalExceptionHandler`와 Flyway V1 baseline을 추가했다.
- `.env.example`, Dockerfile, GitHub Actions CI와 루트 README를 작성했다.

## 검증 결과

- `./gradlew test` 통과
- `cd frontend && npm test` 통과
- `docker compose up --build`로 전체 서비스 기동 및 헬스 체크 통과

## 남은 사항

- 없음. 업무 기능은 단계 1부터 구현한다.
