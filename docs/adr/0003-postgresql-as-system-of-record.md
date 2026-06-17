# 0003. PostgreSQL을 시스템 오브 레코드로 사용

- **Status**: Accepted
- **Date**: 2026-06-18

## Context

금융 시스템의 원장/계정계는 강한 정합성(ACID 트랜잭션)과 정밀한 수치 표현이 필수다.
또한 도메인 불변식을 DB 제약으로도 한 번 더 방어(심층 방어)하고 싶다.

## Decision

주 데이터스토어로 **PostgreSQL**을 사용한다. 금액은 `bigint`(원 단위 정수)로 저장하고,
도메인 불변식은 `CHECK` 제약으로 DB에서도 보강한다(예: `raised_amount <= target_amount`).

## Consequences

- **긍정**: 강한 트랜잭션 보장, `SELECT … FOR UPDATE`(비관적 락) 지원, 풍부한 제약 조건.
  PFCT 기술 스택에도 포함되어 현업 친화적.
- **부정/비용**: 통합 테스트에 실제 DB가 필요(→ Testcontainers + Docker 의존).
- **후속**: 스키마는 Flyway로 관리하고 JPA는 `ddl-auto: validate`로 검증만 한다(ADR-0007 예정).

## Alternatives considered

- **MySQL** — 동등하게 가능하나, 금융 정밀도/제약 조건 표현과 `FOR UPDATE` 세분 옵션에서 PostgreSQL을 선호.
- **H2(인메모리)** — 테스트는 빠르지만 실제 DB와 동작이 달라 정합성 검증의 신뢰도가 낮음.
