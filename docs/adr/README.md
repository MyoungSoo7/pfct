# Architecture Decision Records (ADR)

이 폴더는 이 프로젝트의 **중요한 설계 결정**을 기록합니다. ADR은 "무엇을, 왜, 어떤 대안을 제치고"
결정했는지를 남겨, 나중에(혹은 면접에서) 의사결정의 맥락을 설명할 수 있게 합니다.

> 형식은 [Michael Nygard 스타일](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)을 따릅니다.

## 목록

| # | 제목 | 상태 |
|---|------|------|
| [0001](0001-use-adrs-to-record-decisions.md) | ADR로 설계 결정을 기록한다 | Accepted |
| [0002](0002-modular-monolith-with-clean-architecture.md) | 멀티모듈 모듈러 모놀리스 + 클린 아키텍처 | Accepted |
| [0003](0003-postgresql-as-system-of-record.md) | PostgreSQL을 시스템 오브 레코드로 사용 | Accepted |
| [0004](0004-double-entry-ledger-for-money-movement.md) | 모든 자금 이동은 복식부기 원장으로 | Accepted |
| [0005](0005-pessimistic-locking-for-funding-concurrency.md) | 펀딩 동시성에 비관적 락 사용 | Accepted |
| [0006](0006-money-value-object-with-bigdecimal.md) | 금액은 BigDecimal 기반 Money VO로 | Accepted |
| [0007](0007-idempotent-append-only-ledger-writes.md) | 멱등한 append-only 원장 기록 | Accepted |

## 새 ADR 추가 방법

1. `0000-template.md`를 복사해 `NNNN-제목.md`로 만든다 (NNNN은 다음 번호).
2. 내용을 채우고 `Status`를 정한다 (`Proposed` / `Accepted` / `Superseded by ADR-XXXX`).
3. 위 표에 한 줄 추가한다.
4. **결정 기록 프로토콜**(`CLAUDE.md` 참고)에 따라 `MEMORY.md`와 `STATUS.md`도 함께 갱신한다.
