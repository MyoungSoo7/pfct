# 0009. 이벤트 발행은 트랜잭셔널 아웃박스로

- **Status**: Accepted
- **Date**: 2026-06-18

## Context

상태 변경(예: 원장 지급 기록)과 그에 따른 이벤트 발행(예: `LoanDisbursed`)을 따로 처리하면
**이중 쓰기 문제**가 생긴다 — DB는 커밋됐는데 메시지 발행이 실패하거나, 그 반대가 되어 둘이 불일치한다.

## Decision

**트랜잭셔널 아웃박스 패턴**을 도입한다(`modules/outbox` 플랫폼 모듈).

- 이벤트를 상태 변경과 **같은 트랜잭션**으로 `outbox_event` 테이블에 적재한다(`OutboxRecorder`).
- 별도 릴레이(`OutboxRelay`, `@Scheduled`)가 미발행 이벤트를 폴링해 발행하고 `published_at` 을 찍는다.
- 발행 실패 시 `published_at` 이 null로 남아 다음 폴링에서 재시도된다(**at-least-once**).
- 발행 대상은 `EventPublisher` 인터페이스로 추상화한다(현재 로깅 구현 → Phase C에서 Kafka로 교체).

지급 단계는 원장 기록이 실제로 반영된 경우(`applied=true`)에만 아웃박스에 적재하여 **멱등**을 유지한다.

## Consequences

- **긍정**: 상태와 이벤트가 원자적으로 함께 커밋되어 유실/불일치가 없다. 소비자는 at-least-once를 가정하고
  멱등 처리하면 된다. 발행 대상 교체가 인터페이스 한 곳으로 격리된다.
- **부정/비용**: 폴링 지연만큼 발행이 늦다. 아웃박스 테이블 정리(보관/삭제) 정책이 필요하다.
  소비자 측 멱등성이 전제되어야 한다.
- **후속**: `EventPublisher` 의 Kafka 구현, 발행 완료 이벤트 보관/아카이브, 컨슈머 멱등 처리(Phase C).

## Alternatives considered

- **상태 변경 후 직접 발행(2-phase 없이)** — 이중 쓰기 불일치 위험.
- **CDC(Debezium 등)** — 강력하지만 인프라 부담이 큼. 애플리케이션 레벨 아웃박스로 충분.
