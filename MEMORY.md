# MEMORY — 결정 로그 & 핵심 지식

이 파일은 프로젝트의 **누적 기억**이다. 중요한 설계 결정과 변하지 않는 규칙을 한 줄씩 기록한다.
상세 근거는 `docs/adr/`의 ADR에, 현재 진행 상태는 `STATUS.md`에 둔다.

> 갱신 규칙은 `CLAUDE.md`의 **결정 기록 프로토콜**을 따른다.

## 결정 로그 (Decision Log)

| 날짜 | ADR | 결정 요약 |
|------|-----|-----------|
| 2026-06-18 | [0001](docs/adr/0001-use-adrs-to-record-decisions.md) | 설계 결정을 ADR로 기록 |
| 2026-06-18 | [0002](docs/adr/0002-modular-monolith-with-clean-architecture.md) | 멀티모듈 모듈러 모놀리스 + 클린 아키텍처 (domain→application→adapter) |
| 2026-06-18 | [0003](docs/adr/0003-postgresql-as-system-of-record.md) | 주 데이터스토어 PostgreSQL, 금액은 bigint(원), CHECK 제약 |
| 2026-06-18 | [0004](docs/adr/0004-double-entry-ledger-for-money-movement.md) | 모든 자금 이동은 복식부기 원장(차변=대변 불변식) |
| 2026-06-18 | [0005](docs/adr/0005-pessimistic-locking-for-funding-concurrency.md) | 펀딩 동시성은 비관적 락(SELECT … FOR UPDATE) |
| 2026-06-18 | [0006](docs/adr/0006-money-value-object-with-bigdecimal.md) | 금액은 BigDecimal 기반 Money VO, 이자율은 AnnualInterestRate VO |
| 2026-06-18 | [0007](docs/adr/0007-idempotent-append-only-ledger-writes.md) | 원장은 append-only, 거래 ID를 멱등 키로(check-first + PK 백스톱), 잔액은 Σ분개로 도출 |
| 2026-06-18 | [0008](docs/adr/0008-orchestrated-saga-for-loan-execution.md) | 대출 실행은 오케스트레이션 Saga(독립 트랜잭션 단계 + 역순 보상), 오케스트레이터는 컴포지션 루트에 |
| 2026-06-18 | [0009](docs/adr/0009-transactional-outbox-for-event-publishing.md) | 이벤트는 트랜잭셔널 아웃박스(상태 변경과 같은 tx 적재 → 릴레이 발행, at-least-once) |
| 2026-06-18 | [0010](docs/adr/0010-pro-rata-settlement-largest-remainder.md) | 정산은 개별 투자 내역 기반 최대 잉여 비율 분배(합 보존) + 이자 수수료 + 원장 기록(멱등) |
| 2026-06-18 | [0011](docs/adr/0011-kafka-as-event-transport.md) | 이벤트 전송은 Kafka(KafkaEventPublisher @Primary, 토픽 pfct.outbox, 키=aggregateId, at-least-once) |
| 2026-06-18 | [0012](docs/adr/0012-cqrs-read-model-via-events.md) | 투자자 수익은 CQRS 읽기 모델(정산 이벤트 → 프로젝터 → investor_return_view, processed_event로 멱등) |
| 2026-06-20 | [0013](docs/adr/0013-delinquency-and-overdue-scanning.md) | 연체는 상태 있는 상환 회차(loan_repayment, DUE/PAID/OVERDUE)로 영속화 + 주기 스캔(@Scheduled, 회차당 독립 tx)으로 OVERDUE 전이·연체료(정수 연산, 절사) + RepaymentOverdue 아웃박스 이벤트 |
| 2026-06-20 | [0014](docs/adr/0014-outbox-retry-backoff-and-dlq.md) | 아웃박스 릴레이는 이벤트 단위 처리 + 지수 백오프 재시도(attempts/next_attempt_at) + 한도 초과 시 DLQ 격리(dead 플래그=권위, pfct.outbox.dlq 토픽=부가, 운영 조회 GET /api/admin/outbox/dead) |
| 2026-06-20 | [0015](docs/adr/0015-skip-locked-multi-instance-outbox-relay.md) | 아웃박스 릴레이 후보 조회는 FOR UPDATE SKIP LOCKED(PESSIMISTIC_WRITE + lock.timeout=-2)로 — 여러 릴레이 인스턴스가 disjoint 배치를 가져가 중복 발행 없이 수평 확장(select→발행→published_at 갱신이 한 tx) |

## 변하지 않는 규칙 (Conventions)

- **돈은 절대 Double/Float 금지.** 항상 `Money`(원 단위 정수) 사용.
- **`domain` 패키지에 Spring/JPA import 금지.** 프레임워크는 `adapter`에서만.
- **의존성 방향**: `adapter → application → domain`. 역방향 금지.
- **스키마 변경은 Flyway 마이그레이션으로만.** JPA는 `ddl-auto: validate`.
- **자금 이동은 반드시 `LedgerTransaction`을 통해서만** 원장에 기록(차변=대변).
- **빌드/스택**: Kotlin 1.9.25 · Java 21 · Spring Boot 3.5.15 · Gradle 8.14.5. 전 모듈 통일.

## 핵심 좌표 (Where things live)

- `common/` — Money, AnnualInterestRate, DomainEvent
- `modules/ledger/` — 복식부기 원장(계정계)
- `modules/investment/` — 펀딩/투자 (도메인 + JPA 영속화 + 비관적 락)
- `modules/lending/` — 여신/대출 + 원리금균등 상환 스케줄 + Loan 영속화 + 상태 있는 상환 회차(`ScheduledRepayment`, `DelinquencyCalculator`)
- `modules/outbox/` — 트랜잭셔널 아웃박스 플랫폼(레코더 + 릴레이[백오프 재시도/DLQ] + 퍼블리셔 + 데드레터)
- `modules/settlement/` — 정산 비율 분배기(ProRataDistributor, 순수 도메인)
- `bootstrap/` — Spring Boot 조립, REST, 대출 실행 Saga(`saga/`), 정산(`settlement/`), 연체 스캔(`overdue/`), 통합 테스트(Testcontainers)
