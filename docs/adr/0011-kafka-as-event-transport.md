# 0011. Kafka 를 이벤트 전송 수단으로

- **Status**: Accepted
- **Date**: 2026-06-18

## Context

ADR-0009 에서 아웃박스에 이벤트를 적재하고 릴레이가 발행하기로 했고, 발행 대상은 `EventPublisher`
인터페이스로 추상화해 두었다(초기엔 로깅 구현). 컨텍스트 간 비동기 통신과 외부 소비를 위해
실제 메시지 브로커가 필요하다.

## Decision

이벤트 전송 수단으로 **Apache Kafka** 를 사용한다.

- `KafkaEventPublisher`(@Primary)가 `EventPublisher` 를 구현해 아웃박스 이벤트를 토픽 `pfct.outbox` 로 발행.
- **키 = aggregateId** → 같은 애그리거트의 이벤트는 같은 파티션으로 가 순서가 보장된다.
- 발행 ack 를 동기로 대기 → 실패 시 `published_at` 미기록 → 릴레이가 재시도(at-least-once).
- 헤더에 `eventType`/`aggregateType` 을 실어 소비자가 역직렬화 없이 라우팅할 수 있게 한다.
- 통합 테스트는 Testcontainers Kafka(@ServiceConnection)로 발행→구독 전 구간을 검증한다.

## Consequences

- **긍정**: 컨텍스트 간 결합도가 낮아지고, 외부 시스템이 이벤트를 구독할 수 있다. 파티션 키로 순서 보장.
  `EventPublisher` 교체만으로 로깅 → Kafka 전환(기존 아웃박스/Saga 코드 무변경).
- **부정/비용**: 인프라(브로커) 운영 부담. 소비자는 at-least-once 전제로 **멱등 처리** 필수.
- **후속**: 소비자가 이벤트로 CQRS 읽기 모델을 갱신(ADR 예정). 스키마(Avro/JSON Schema)·DLQ·컨슈머 그룹 정교화.

## Alternatives considered

- **RabbitMQ** — 큐 기반으로 단순하나, 로그 기반 재처리/파티션 순서/대용량 스트리밍은 Kafka 가 유리.
  PFCT 스택에도 Kafka 포함.
- **DB 폴링만으로 소비** — 아웃박스를 소비자가 직접 읽는 방식은 결합도가 높고 확장이 어렵다.
