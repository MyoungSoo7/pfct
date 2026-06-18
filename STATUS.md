# STATUS — 진행 상태

> 현재 어디까지 했고 다음에 무엇을 하는지. 결정의 *맥락*은 `docs/adr/`, *누적 기억*은 `MEMORY.md` 참고.

**마지막 갱신**: 2026-06-18 (Phase C 완료)

## 스냅샷

- **빌드**: ✅ `gradlew build` GREEN
- **테스트**: ✅ 통합 13개(동시성·원장×2·Saga×2·보상·정산×2·Kafka·**CQRS×2**·contextLoads)
  + 단위(ProRataDistributor 6, 도메인 다수) + ArchUnit 계층 규칙 3개
- **스택**: PostgreSQL · Apache Kafka · Flyway · JPA · Testcontainers (모두 Testcontainers로 검증)
- **스택**: Kotlin 1.9.25 · Java 21 · Spring Boot 3.5.15 · PostgreSQL · Flyway · Testcontainers
- **원격**: https://github.com/MyoungSoo7/pfct (master, public)

## 단계별 진행

### ✅ Phase A — 도메인 코어 (완료)
- `common`: Money / AnnualInterestRate / DomainEvent
- `ledger`: 복식부기 `LedgerTransaction`(차변=대변 불변식)
- `investment`: `FundingRound`(오버펀딩 금지 불변식)
- `lending`: `EqualPaymentScheduleGenerator`(원리금균등, 원금 합계 정확 일치)
- 순수 단위 테스트, Docker 불필요. 커밋 `ebad7d7`.

### ✅ Phase B1 — 투자 영속화 + 동시성 정합성 (완료, 커밋 대기)
- 루트 빌드 정리(전 모듈 Kotlin 1.9.25/Java 21 통일)
- `investment` JPA 영속화: 엔티티 + 비관적 락 리포지토리 + 어댑터/매퍼
- `InvestService`/`OpenFundingRoundService`(`@Transactional`)
- REST: `FundingRoundController` + `ApiExceptionHandler`(409/404)
- Flyway `V1__create_funding_round.sql`(CHECK 제약 = 심층 방어)
- Testcontainers 싱글턴 + **`FundingConcurrencyTest`: 200스레드 → 100 성공, 오버펀딩 0 ✅**

### ✅ Phase B2 — 원장 영속화 + Saga (완료)
- [x] `ledger` append-only 영속화 + `RecordLedgerTransaction` 유스케이스 (B2-1)
- [x] 멱등성(거래 ID = 멱등 키, check-first + PK 백스톱, ADR-0007) (B2-1)
- [x] `lending` Loan 영속화 + `CreateLoanService`(멱등/취소) (B2-2)
- [x] 대출 실행 Saga: 라운드 실행확정 → 대출 생성 → 지급 + 역순 보상 (ADR-0008) (B2-2)
- [x] Outbox 패턴(`modules/outbox`, 같은 tx 적재 + @Scheduled 릴레이, ADR-0009) (B2-2)
- [x] 통합 테스트: 정상 실행 / 보상(롤백) / 멱등 재실행 (B2-2)

### ✅ Phase C — 정산 / EDA / CQRS (완료)
- [x] 정산(상환금 투자 비율 분배 + 수수료, 최대 잉여 방식, ADR-0010) (C-1)
- [x] 개별 투자 내역 영속화(`investment` 테이블, Flyway V5) (C-1)
- [x] ArchUnit 계층 의존 규칙 강제 테스트 (도메인 프레임워크 무의존 + adapter→application→domain) (C-2)
- [x] Kafka 이벤트 발행/구독(`KafkaEventPublisher` @Primary + 컨슈머, 토픽 pfct.outbox, ADR-0011) (C-3)
- [x] CQRS 읽기 모델(투자자 수익 — 정산 이벤트 구독 프로젝터, 읽기측 멱등, ADR-0012) (C-4)
- [x] README 아키텍처 다이어그램(mermaid) + 실행 가이드 + AI 활용 사례 + `docker-compose.yml` (C-5)

## CI
- GitHub Actions(`.github/workflows/ci.yml`): push/PR 시 `gradlew build`(통합 테스트 포함) 실행.
  ubuntu 러너의 Docker 로 Testcontainers(Postgres/Kafka) 동작.

## 알려진 메모
- 통합 테스트는 Docker 필요(모듈 단위 테스트는 불필요).
- `docs/` 는 한때 `.gitignore`에 잡혀 있었음 → 제거함(ADR 추적 위해).
