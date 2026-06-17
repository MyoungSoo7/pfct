# 0006. 금액은 BigDecimal 기반 Money VO로

- **Status**: Accepted
- **Date**: 2026-06-18

## Context

금융 계산에서 `Double`/`Float`를 쓰면 부동소수점 오차로 1원이 사라지거나 생긴다.
또한 금액을 원시 `Long`으로 흩뿌리면 통화 단위/연산 규칙이 코드 전반에 중복·누락된다.

## Decision

금액을 `Money` **Value Object**로 캡슐화한다. 내부적으로 원(KRW) 단위 정수(`BigDecimal`, scale=0)만 허용하고,
원 미만 소수가 들어오면 생성 시 거부한다. 이자 계산 등 중간 연산은 `BigDecimal` + 명시적 `RoundingMode`로 수행한다.
연이자율은 `AnnualInterestRate` VO로 표현하며 법정 최고금리(연 20%)를 생성 시 강제한다.

## Consequences

- **긍정**: 부동소수점 오차가 타입 수준에서 차단된다. 통화 연산 규칙이 한 곳에 응집된다.
  원리금균등 스케줄에서 "원금 합계 == 대출 원금"이 1원도 어긋나지 않음을 테스트로 보장.
- **부정/비용**: 원시 타입보다 약간의 래핑 비용. DB 저장 시 `Long`과의 매핑 코드가 필요.
- **후속**: 다통화 지원이 필요해지면 통화 코드 필드를 Money에 추가.

## Alternatives considered

- **Long(원 단위) 직접 사용** — 가볍지만 규칙이 분산되고 의미가 약함.
- **Double/Float** — 금융에서 금지. 오차로 정합성이 깨짐.
