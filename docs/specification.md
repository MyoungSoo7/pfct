# PFCT 명세서 (Specification)

> 미니 P2P 대출·투자 플랫폼 — 계정계 백엔드.
> 이 문서는 **무엇을 만드는가(요구사항)**와 **어떻게 만드는가(설계·계약)**를 한곳에 정리한 단일 진실 원천이다.
> 결정의 *근거*는 [`docs/adr/`](adr/), *진행 상태*는 [`STATUS.md`](../STATUS.md), *누적 결정 로그*는 [`MEMORY.md`](../MEMORY.md)에 있다.

- **문서 버전**: 1.0
- **작성일**: 2026-06-20
- **대상 코드 상태**: Phase A·B·C·D 완료 (`gradlew build` GREEN, 2026-06-20)

---

## 1. 개요

### 1.1 목적

PFCT는 투자자가 자금을 모아 차주에게 대출하고, 차주의 상환금을 투자 비율대로 정산하는
**미니 P2P 대출·투자 플랫폼의 계정계(system of record)**다.
포트폴리오로서의 목표는 단 하나로 수렴한다 — **"돈이 한 푼도 생기거나 사라지지 않는다."**
이를 *타입*과 *테스트*로 증명하는 것이 이 프로젝트의 본질이다.

### 1.2 핵심 가치(설계 불변식)

| # | 불변식 | 강제 수단 |
|---|--------|-----------|
| V1 | 금액은 절대 부동소수점이 아니다 | `Money` VO(BigDecimal 정수, scale=0) — `Double`/`Float` 금지 |
| V2 | 모든 자금 이동은 차변=대변이 같다 | 복식부기 `LedgerTransaction`(불균형 시 생성 거부) |
| V3 | 펀딩 모집 총액은 목표를 넘지 않는다 | 비관적 락(`SELECT … FOR UPDATE`) + DB CHECK |
| V4 | 같은 거래는 두 번 반영되지 않는다 | 거래 ID = 멱등 키(append-only + PK 백스톱) |
| V5 | 상태 변경과 이벤트 발행은 원자적이다 | 트랜잭셔널 아웃박스(같은 tx 적재) |
| V6 | 분배 합은 원본 합과 정확히 같다 | 최대 잉여(largest-remainder) 비율 분배 |
| V7 | 이자율은 법정 최고금리를 넘지 않는다 | `AnnualInterestRate` VO(생성 시 연 20% 강제) |

### 1.3 범위

**포함(In scope)**: 펀딩 모집 · 투자 · 대출 실행(Saga) · 원리금균등 상환 스케줄 ·
상환 정산(비율 분배 + 수수료) · 복식부기 원장 · 이벤트 발행(아웃박스→Kafka) ·
투자자 수익 조회(CQRS) · 연체 감지/연체료 계산(Phase D).

**제외(Out of scope)**: 인증/인가, 회원/KYC, 실제 PG·은행 연동, 세금 처리,
프런트엔드 UI, 다통화(현재 KRW 단일). 계정계의 정합성 증명에 집중한다.

---

## 2. 도메인 모델

### 2.1 유비쿼터스 언어(Ubiquitous Language)

| 용어 | 의미 |
|------|------|
| FundingRound(펀딩 라운드) | 한 대출 건을 위해 자금을 모집하는 단위. 목표액·모집액·상태를 가짐 |
| Investment(투자) | 한 투자자가 한 라운드에 넣은 개별 출자 내역(정산 비율의 근거) |
| Loan(대출) | 모집 완료된 자금으로 차주에게 실행된 여신. 원금·이자율·기간 보유 |
| RepaymentInstallment | 원리금균등으로 계산된 **순수 스케줄 회차(값)** |
| ScheduledRepayment | 상태(DUE/PAID/OVERDUE)를 가진 **영속 상환 회차** |
| Settlement(정산) | 차주 상환금을 투자 비율대로 분배 + 플랫폼 수수료 차감 |
| LedgerTransaction | 차변=대변 분개의 묶음. 모든 자금 이동의 원자 단위 |
| Outbox | 상태 변경과 같은 tx로 적재되는 발행 대기 이벤트 |
| 연체(Delinquency) | 납기를 지난 미상환 회차. 연체료(지연배상금) 발생 |

### 2.2 핵심 도메인 타입

- **`Money`** (common) — KRW 정수 금액 VO. `won(Long)`/`won(BigDecimal)` 생성,
  `+`/`-`/`*` 연산, 부호 판정. 1원 미만 입력은 생성 거부.
- **`AnnualInterestRate`** (common) — 연이자율 VO. 생성 시 `0 ≤ rate ≤ 20%` 강제,
  `monthlyRate()`로 월이자율(소수 12자리) 제공.
- **`DomainEvent`** (common) — 도메인 이벤트 마커.
- **`LedgerTransaction` / `JournalEntry` / `AccountId`** (ledger) — 복식부기 원장.
  분개 차변 합 ≠ 대변 합이면 거래를 만들 수 없다.
- **`FundingRound`** (investment) — 오버펀딩 금지 불변식 보유. 상태 전이(OPEN→EXECUTED 등).
- **`Loan`** (lending) — 대출 애그리거트. 약정 이자율·원금·기간.
- **`EqualPaymentScheduleGenerator` / `RepaymentInstallment`** (lending) —
  원리금균등 상환 스케줄 생성. **원금 합계가 원본과 정확히 일치**(잔차는 마지막 회차 보정).
- **`ScheduledRepayment`** (lending, Phase D) — 상태 있는 상환 회차.
  `markPaid()`(멱등), `markOverdue(asOf, rate)`(DUE이고 납기 경과일 때만 전이, 멱등).
- **`DelinquencyCalculator`** (lending, Phase D) — 연체료 = 연체원리금 × 연체이자율 × 연체일수 / 365,
  **원 단위 절사**, 전 과정 BigDecimal 정수 연산.
- **`ProRataDistributor`** (settlement) — 최대 잉여 방식 비율 분배(분배 합 = 원본 합).

---

## 3. 아키텍처

### 3.1 구조 원칙 (ADR-0002)

- **멀티모듈 모듈러 모놀리스.** 바운디드 컨텍스트 = Gradle 모듈, 계층 = 패키지.
- 의존성 방향은 **항상 `adapter → application → domain`** (역방향 금지, ArchUnit으로 강제).
- `domain` 패키지는 **순수 코틀린** — Spring/JPA import 금지. 프레임워크는 `adapter`에서만.
- 컨텍스트 간 통신은 **이벤트**로 (직접 호출 최소화).
- 조립(컴포지션 루트)·REST·Saga 오케스트레이션·통합 테스트는 `bootstrap`에만.

### 3.2 모듈 맵

| 모듈 | 책임 | 프레임워크 의존 |
|------|------|-----------------|
| `common` | Money, AnnualInterestRate, DomainEvent | 없음 |
| `modules/ledger` | 복식부기 원장(계정계) | adapter만 JPA |
| `modules/investment` | 펀딩/투자 — 오버펀딩 금지, 비관적 락, 개별 투자 내역 | adapter만 JPA |
| `modules/lending` | 여신/대출 — 원리금균등 스케줄, 상환 회차, 연체료 | adapter만 JPA |
| `modules/settlement` | 정산 비율 분배기(최대 잉여, 합 보존) | 없음(순수 도메인) |
| `modules/outbox` | 트랜잭셔널 아웃박스 플랫폼(레코더 + 릴레이 + Kafka 발행) | Spring/JPA/Kafka |
| `bootstrap` | Spring Boot 조립, REST, 대출 실행 Saga, 정산, CQRS 프로젝터, 연체 스캐너 | 전부 |

### 3.3 자금의 일생 (엔드투엔드 흐름)

```
투자자 ──투자──▶ [펀딩/투자]
                     │ 모집완료
                     ▼
              ┌─ 대출 실행 Saga ─┐
              │ 1. 라운드 실행확정 │
              │ 2. 대출 생성       │  ← 실패 시 역순 보상(2→1)
              │ 3. 지급 + 이벤트   │
              └────────┬─────────┘
                       ▼
               [복식부기 원장] ◀──┐
                                  │ 투자 비율 분배 + 수수료
차주 ──원리금 상환──▶ [정산] ──────┘
                       │ RepaymentSettled
                       ▼
                  [아웃박스] ──릴레이──▶ Kafka ──프로젝터──▶ [투자자 수익 읽기 모델(CQRS)]

(Phase D) 연체 스캐너 ──@Scheduled──▶ 납기초과 회차 OVERDUE 전이 + 연체료 + RepaymentOverdue 이벤트(아웃박스)
```

---

## 4. 정합성·동시성 메커니즘 (핵심 설계 결정)

각 항목은 ADR에 근거·대안과 함께 기록돼 있다.

| 주제 | 결정 | 근거 요약 | ADR |
|------|------|-----------|-----|
| 시스템 오브 레코드 | PostgreSQL, 금액은 bigint(원), CHECK 제약 | 단순·강한 정합성·심층 방어 | [0003](adr/0003-postgresql-as-system-of-record.md) |
| 정합성 | 복식부기 원장(차변=대변 불변식) | 자금 이동을 원자·검증가능하게 | [0004](adr/0004-double-entry-ledger-for-money-movement.md) |
| 동시성 | 펀딩 막차 경쟁에 비관적 락 | 오버펀딩을 락으로 0 보장 | [0005](adr/0005-pessimistic-locking-for-funding-concurrency.md) |
| 금액 | BigDecimal 기반 Money VO | 부동소수점 오차 원천 차단 | [0006](adr/0006-money-value-object-with-bigdecimal.md) |
| 멱등 | 거래 ID = 멱등 키(append-only) | 재실행/중복 발행에도 1회 반영 | [0007](adr/0007-idempotent-append-only-ledger-writes.md) |
| 분산 트랜잭션 | 오케스트레이션 Saga + 역순 보상 | 2PC 없이 단계별 정합성 | [0008](adr/0008-orchestrated-saga-for-loan-execution.md) |
| 이중 쓰기 | 트랜잭셔널 아웃박스 | DB커밋과 이벤트발행 원자화(at-least-once) | [0009](adr/0009-transactional-outbox-for-event-publishing.md) |
| 정산 | 최대 잉여 비율 분배 + 수수료 + 원장 기록 | 분배 합 보존, 1원도 안 샘 | [0010](adr/0010-pro-rata-settlement-largest-remainder.md) |
| EDA | Kafka(키=aggregateId, at-least-once) | 순서 보장·확장 가능 전송 | [0011](adr/0011-kafka-as-event-transport.md) |
| 조회 | 이벤트 기반 CQRS 읽기 모델 | 쓰기/읽기 분리, 읽기측 멱등 | [0012](adr/0012-cqrs-read-model-via-events.md) |

**대출 실행 Saga 단계**: ① 펀딩 라운드 실행확정 → ② 대출 생성 → ③ 자금 지급 + 이벤트 적재.
각 단계는 **독립 트랜잭션**이며, 실패 시 완료된 단계를 **역순으로 보상**한다.
멱등 키로 재실행 시 중복 효과가 없다.

---

## 5. 데이터 모델 (Flyway 마이그레이션)

스키마 변경은 **Flyway로만**, JPA는 `ddl-auto: validate`. 금액은 모두 `bigint`(원).

| 버전 | 테이블 | 핵심 제약/인덱스 |
|------|--------|------------------|
| V1 | `funding_round` | 모집액 ≤ 목표액 CHECK(심층 방어) |
| V2 | `ledger_transaction`, `journal_entry` | append-only, 거래 ID 유니크 |
| V3 | `loan` | 대출 애그리거트 |
| V4 | `outbox_event` | 발행 상태/순서 |
| V5 | `investment` | 개별 투자 내역(정산 비율 근거) |
| V6 | 읽기 모델(`investor_return_view`, `processed_event`) | 읽기측 멱등 |
| V7 | `loan_repayment` | `(loan_id, sequence)` 유니크, status CHECK, `(status, due_date)` 인덱스(연체 후보 조회) |

---

## 6. API 명세

베이스: REST/JSON. 오류는 `ApiExceptionHandler`가 매핑(중복/충돌 409, 미존재 404).

| 메서드 | 경로 | 설명 | 정합성 포인트 |
|--------|------|------|----------------|
| `POST` | `/api/funding-rounds` | 펀딩 라운드 개설 | — |
| `POST` | `/api/funding-rounds/{id}/investments` | 투자 | 비관적 락으로 오버펀딩 차단 |
| `POST` | `/api/loans/execute` | 대출 실행 | 오케스트레이션 Saga + 보상 |
| `POST` | `/api/loans/{loanId}/settlements` | 상환 정산 | 비율 분배 + 수수료, 멱등 |
| `GET`  | `/api/investors/{id}/returns` | 투자자 수익 조회 | CQRS 읽기 모델 |
| `GET`  | `/api/loans/{loanId}/repayments` | 상환 스케줄/연체 상태 조회 (Phase D) | 회차별 status·late_fee |

**응답 예시 — 상환 스케줄 조회** (`RepaymentView`):
```json
[
  { "sequence": 1, "dueDate": "2026-07-20", "principal": 833333, "interest": 16666, "lateFee": 0, "status": "PAID" },
  { "sequence": 2, "dueDate": "2026-08-20", "principal": 833333, "interest": 13888, "lateFee": 2300, "status": "OVERDUE" }
]
```

---

## 7. 이벤트 명세

상태 변경과 같은 트랜잭션으로 아웃박스에 적재 → 릴레이가 Kafka로 발행(at-least-once,
키 = aggregateId). 컨슈머는 멱등 처리.

| 이벤트 | 발행 시점 | 페이로드(요지) | 구독자 |
|--------|-----------|----------------|--------|
| `RepaymentSettled` | 상환 정산 완료 | 대출/회차, 투자자별 분배액, 수수료 | 투자자 수익 프로젝터(CQRS) |
| `RepaymentOverdue` (Phase D) | 회차 연체 전이 | loanId, sequence, overdueAmount, lateFee, dueDate | (확장 지점 — 알림/추심 등) |

토픽: `pfct.outbox`.

---

## 8. Phase D — 연체/상환 스케줄 (완료)

> 기존 패턴(멱등·트랜잭셔널 아웃박스·@Scheduled 릴레이)을 그대로 재사용한다. 근거는 [ADR-0013](adr/0013-delinquency-and-overdue-scanning.md).

### 8.1 요구사항

- 대출 실행 시 원리금균등 스케줄을 `loan_repayment`로 영속화한다(상태 DUE).
- 정산되면 해당 회차는 `PAID`로 전이(멱등).
- 납기를 지난 DUE 회차는 주기 스캔으로 `OVERDUE` 전이하고 **연체료**를 계산한다.
- 연체 전이는 상태 변경 + `RepaymentOverdue` 이벤트 적재를 **한 트랜잭션**으로 묶는다.

### 8.2 연체료 규칙

```
연체료 = 연체원리금(원금+이자) × 연체이자율(연) × 연체일수 / 365   (원 단위 절사)
```
- 연체이자율은 대출의 약정 이자율을 사용한다.
- 전 과정 BigDecimal 정수 연산 — 1원 오차도 없음.
- `daysOverdue ≤ 0`이면 0원.

### 8.3 동작

1. `OverdueScanner` — `@Scheduled(fixedDelay=pfct.overdue.scan-delay-ms, 기본 60s)`로
   `findOverdueCandidates(today)`(status=DUE & 납기경과)를 조회.
2. 각 후보를 `OverdueProcessingService.process()`에서 **독립 트랜잭션**으로 처리
   (한 건 실패가 전체를 막지 않음).
3. `ScheduledRepayment.markOverdue()`가 멱등 가드(이미 OVERDUE/PAID거나 미연체면 무시).
4. 전이 시 회차 갱신 + `RepaymentOverdue` 아웃박스 적재.

### 8.4 완료 기준(DoD)

- [x] 대출 실행 Saga가 상환 스케줄을 함께 영속화 (`CreateLoanService`)
- [x] 정산 시 해당 회차 PAID 전이 연결 (`SettleRepaymentService`, applied=true일 때만)
- [x] `DelinquencyTest`(연체료 계산) GREEN
- [x] `OverdueIntegrationTest`(스캔→전이→이벤트) GREEN
- [x] ADR-0013 추가 + ADR 인덱스 + MEMORY.md + STATUS.md 갱신(결정 기록 프로토콜)

---

## 9. 품질·테스트 전략

| 계층 | 도구 | 검증 대상 | Docker |
|------|------|-----------|--------|
| 순수 단위 | Kotlin/JUnit | 도메인 불변식, 스케줄·분배·연체료 계산 | 불필요 |
| 통합 | Testcontainers (Postgres/Kafka) | 정합성을 *실제로 증명* | 필요 |
| 아키텍처 | ArchUnit | 계층 의존 규칙(컴파일 클래스 기준) | 불필요 |

**대표 통합 테스트(정합성 증명):**
- `FundingConcurrencyTest` — 200스레드 동시 투자 → 정확히 100건 성공, **오버펀딩 0**.
- 대출 실행 Saga — 정상 / 보상(롤백) / 멱등 재실행.
- 아웃박스 → Kafka → CQRS 읽기 모델 갱신.
- 정산 — 비율 분배 합 보존 + 수수료(증분 측정으로 테스트 격리).
- (Phase D) 연체 스캔 → OVERDUE 전이 → 이벤트.

**완료 기준(전역 DoD):** `./gradlew build` GREEN(증거 확인) ·
설계 결정이 있었다면 ADR + README 인덱스 + MEMORY.md + STATUS.md 갱신 · 날짜는 절대 날짜.

---

## 10. 기술 스택 · 실행

**스택**: Kotlin 1.9.25 · Java 21 · Spring Boot 3.5.15 · Gradle 8.14.5(멀티모듈) ·
PostgreSQL · Apache Kafka · Flyway · JPA/Hibernate · Testcontainers · ArchUnit.

```bash
docker compose up -d           # 인프라(PostgreSQL + Kafka)
./gradlew :bootstrap:bootRun   # 앱 실행 (DB_URL/DB_USERNAME/DB_PASSWORD/KAFKA_BOOTSTRAP 재정의 가능)

./gradlew build                # 전체 빌드 + 모든 테스트
./gradlew :common:test         # 순수 단위 테스트(Docker 불필요)
./gradlew :bootstrap:test      # 통합 테스트(Docker 필요)
```

**CI**: GitHub Actions(`.github/workflows/ci.yml`) — push/PR 시 `gradlew build`(통합 테스트 포함).

---

## 11. 진행 상태 (요약)

| Phase | 내용 | 상태 |
|-------|------|------|
| A | 도메인 코어(Money/원장/펀딩/스케줄) | ✅ 완료 |
| B1 | 투자 영속화 + 동시성 정합성(비관적 락) | ✅ 완료 |
| B2 | 원장 영속화 + 대출 실행 Saga + 아웃박스 | ✅ 완료 |
| C | 정산 + ArchUnit + Kafka + CQRS | ✅ 완료 |
| D | 연체/상환 스케줄(연체료·스캐너) | ✅ 완료 |

상세는 [`STATUS.md`](../STATUS.md), 결정 로그는 [`MEMORY.md`](../MEMORY.md), 근거는 [`docs/adr/`](adr/) 참고.

---

## 12. 향후 확장 후보 (Backlog)

- `RepaymentOverdue` 구독자: 연체 알림·추심 워크플로.
- 조기 상환/중도 상환 정산.
- 대출 만기/완납 상태 전이 및 라운드 종료.
- 다건 대출에 대한 투자자 포트폴리오 뷰(읽기 모델 확장).
- 멱등 키 기반 외부 PG/은행 연동 어댑터(현재 원장 내부 이체로 추상화).
