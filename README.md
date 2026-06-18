# pfct — 미니 P2P 대출·투자 플랫폼

금융 도메인의 복잡함을 **정확하게** 코드로 풀어내는 것을 목표로 한 백엔드 포트폴리오입니다.
투자 → 대출 실행 → 상환 → 정산으로 이어지는 자금의 일생을, 모든 이동을 **복식부기 원장**에 기록하며
"돈이 한 푼도 생기거나 사라지지 않음"을 타입과 테스트로 보장합니다.

## 핵심 설계

- **DDD + 클린 아키텍처 / 멀티모듈** — 컨텍스트별 모듈, 계층은 `domain → application → adapter`.
  도메인은 프레임워크에 의존하지 않습니다(`common` 모듈엔 Spring 의존성이 한 줄도 없습니다).
- **정합성 우선** — 복식부기 불변식(차변=대변), 비관적 락으로 오버펀딩 차단, DB `CHECK` 제약으로 심층 방어.
- **정확한 금융 계산** — `Money`(원 단위 정수)·`BigDecimal`로 부동소수점 오차 차단,
  원리금균등 상환에서 원금 합계가 1원도 어긋나지 않음을 테스트로 검증.
- **실제 인프라 검증** — Testcontainers로 실제 PostgreSQL에 대해 통합 테스트.
  예: 200스레드 동시 투자에도 오버펀딩 0건임을 증명.

## 기술 스택

Kotlin 1.9.25 · Java 21 · Spring Boot 3.5.15 · Gradle 8.14.5 · PostgreSQL · Flyway · JPA · Testcontainers

## 모듈 구조

| 모듈 | 책임 |
|------|------|
| `common` | Money, AnnualInterestRate, DomainEvent 등 금융 원시 타입 |
| `modules/ledger` | 복식부기 원장(계정계) |
| `modules/investment` | 펀딩/투자 — 오버펀딩 금지 불변식, 비관적 락 영속화 |
| `modules/lending` | 여신/대출 — 원리금균등 상환 스케줄(이자 계산), 대출 영속화 |
| `modules/outbox` | 트랜잭셔널 아웃박스 플랫폼(이벤트 적재 + 릴레이 발행) |
| `bootstrap` | Spring Boot 조립, REST API, 대출 실행 Saga, 통합 테스트 |

## 빌드 & 테스트

```bash
./gradlew build            # 전체 빌드 + 테스트
./gradlew :common:test     # 모듈 단위 테스트 (Docker 불필요)
./gradlew :bootstrap:test  # 통합 테스트 (Docker 필요: Testcontainers)
```

> 통합 테스트는 Docker 데몬이 필요합니다(`postgres:16-alpine` 컨테이너 자동 기동).

## 문서

- [`docs/adr/`](docs/adr/) — 설계 결정 기록(ADR): 왜 이렇게 만들었는지
- [`STATUS.md`](STATUS.md) — 현재 진행 상태와 다음 작업
- [`MEMORY.md`](MEMORY.md) — 누적 결정 로그와 핵심 규칙
- [`CLAUDE.md`](CLAUDE.md) — 기여 지침과 결정 기록 프로토콜

## 진행 상태

Phase A(도메인 코어)·B1(투자 영속화 + 동시성 정합성)·B2(원장·대출 실행 Saga·Outbox) 완료.
상세는 [`STATUS.md`](STATUS.md) 참고.
