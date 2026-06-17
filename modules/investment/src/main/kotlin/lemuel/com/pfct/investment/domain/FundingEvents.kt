package lemuel.com.pfct.investment.domain

import lemuel.com.pfct.common.DomainEvent
import lemuel.com.pfct.common.Money
import java.time.Instant

/** 투자가 약정되었을 때 발생. 원장에 투자자 예치금 → 모집 계정 이동을 일으키는 트리거. */
data class InvestmentMade(
    val roundId: FundingRoundId,
    val investorId: InvestorId,
    val amount: Money,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent

/** 모집 목표 금액이 100% 채워졌을 때 발생. 대출 실행 Saga 의 시작 신호. */
data class FundingFulfilled(
    val roundId: FundingRoundId,
    val totalRaised: Money,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent
