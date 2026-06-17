# CLAUDE.md

이 파일은 이 레포에서 작업하는 Claude(및 사람 기여자)를 위한 운영 지침이다.

## 프로젝트 한 줄 요약

PFCT 백엔드 포지션 포트폴리오 — **미니 P2P 대출·투자 플랫폼**.
"돈이 한 푼도 틀리지 않게" 처리하는 계정계를 DDD/클린 아키텍처/EDA로 구현한다.

## 아키텍처 (요점만)

- **멀티모듈 모듈러 모놀리스.** 컨텍스트 = 모듈, 계층 = 패키지(`domain → application → adapter`).
- `domain`은 **순수 코틀린**(Spring/JPA import 금지). 프레임워크는 `adapter`에서만.
- 자세한 근거는 `docs/adr/` 참고. 현재 상태는 `STATUS.md`, 누적 결정은 `MEMORY.md`.

```
common/        Money, AnnualInterestRate, DomainEvent   (프레임워크 0 의존)
modules/ledger      복식부기 원장(계정계)
modules/investment  펀딩/투자 (+ JPA 영속화, 비관적 락)
modules/lending     여신/대출 (+ 원리금균등 상환 스케줄)
bootstrap/     Spring Boot 조립, REST, 통합 테스트   (유일하게 boot 플러그인)
```

## 빌드 · 테스트

```bash
./gradlew build          # 전체 빌드 + 테스트
./gradlew :common:test   # 모듈 단위 테스트(Docker 불필요)
./gradlew :bootstrap:test # 통합 테스트(Testcontainers → Docker 필요)
```

- 스택: **Kotlin 1.9.25 · Java 21 · Spring Boot 3.5.15 · Gradle 8.14.5 · PostgreSQL**.
- 통합 테스트는 Docker 데몬이 떠 있어야 한다(Testcontainers + `postgres:16-alpine`).

## 코딩 규칙

- 돈은 **항상 `Money`**. `Double`/`Float` 금지. 이자율은 `AnnualInterestRate`.
- 자금 이동은 **반드시 `LedgerTransaction`**(차변=대변)을 통해서만.
- 스키마 변경은 **Flyway 마이그레이션**으로만(`bootstrap/.../db/migration`). JPA는 `validate`.
- 의존성 방향 `adapter → application → domain`을 깨지 않는다.
- 주변 코드의 주석 밀도·네이밍·관용구에 맞춰 작성한다.

## ⭐ 결정 기록 프로토콜 (자동화 규칙)

**중요한 설계/아키텍처 결정을 내릴 때마다, 같은 작업 단위에서 아래를 모두 갱신한다.**
하나라도 빠지면 그 작업은 끝난 것이 아니다.

1. **ADR 작성** — `docs/adr/0000-template.md`를 복사해 `docs/adr/NNNN-제목.md` 생성
   (NNNN = 마지막 번호 + 1). Context / Decision / Consequences / Alternatives를 채운다.
2. **ADR 인덱스 갱신** — `docs/adr/README.md`의 목록 표에 한 줄 추가.
3. **MEMORY.md 갱신** — 결정 로그 표에 `| 날짜 | ADR 링크 | 한 줄 요약 |` 추가.
   불변 규칙이 새로 생기면 "변하지 않는 규칙"에도 반영.
4. **STATUS.md 갱신** — 진행 단계 체크박스/스냅샷을 현재 상태로 갱신, "마지막 갱신" 날짜 수정.
5. **README.md 갱신** — 빌드/실행/구조에 영향이 있으면 반영.

> "중요한 결정"의 기준: 모듈 경계, 영속화/락 전략, 정합성 메커니즘, 외부 의존성 추가/교체,
> 도메인 모델의 핵심 불변식 변경 등. 사소한 리네이밍·포매팅은 제외.

### 완료(Definition of Done) 체크

- [ ] 코드 변경 + 테스트 통과(`./gradlew build` GREEN, 증거 확인)
- [ ] (결정이 있었다면) ADR + README 인덱스 + MEMORY.md + STATUS.md 갱신
- [ ] 날짜는 절대 날짜로 기록(상대 표현 금지)
