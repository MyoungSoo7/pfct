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
- `modules/lending/` — 여신/대출 + 원리금균등 상환 스케줄
- `bootstrap/` — Spring Boot 조립, REST, 통합 테스트(Testcontainers)
