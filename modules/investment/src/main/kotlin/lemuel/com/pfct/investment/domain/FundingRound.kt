package lemuel.com.pfct.investment.domain

import lemuel.com.pfct.common.Money

enum class FundingStatus { OPEN, FULFILLED, CANCELLED }

/**
 * 펀딩 모집(Aggregate Root).
 *
 * 핵심 불변식: **누적 모집액은 목표 금액을 절대 초과할 수 없다(오버펀딩 금지).**
 * 이 규칙을 Aggregate 메서드 안에 가둠으로써, 동시에 다수의 투자자가 "막차" 투자를 시도해도
 * (영속 계층의 락과 결합하면) 목표를 단 1원도 넘기지 않음을 보장한다.
 */
class FundingRound(
    val id: FundingRoundId,
    val targetAmount: Money,
    raised: Money = Money.ZERO,
    status: FundingStatus = FundingStatus.OPEN,
) {
    var raised: Money = raised
        private set

    var status: FundingStatus = status
        private set

    init {
        require(targetAmount.isPositive()) { "모집 목표 금액은 0보다 커야 합니다: $targetAmount" }
        require(raised <= targetAmount) { "초기 모집액이 목표를 초과할 수 없습니다" }
    }

    val remaining: Money get() = targetAmount - raised

    /**
     * 투자를 약정한다.
     * @return 약정 결과로 발생한 도메인 이벤트들 (항상 [InvestmentMade], 목표 달성 시 [FundingFulfilled] 추가)
     * @throws IllegalStateException 모집중이 아닐 때
     * @throws IllegalArgumentException 금액이 0 이하이거나 잔여 모집액을 초과할 때
     */
    fun invest(investorId: InvestorId, amount: Money): List<Any> {
        check(status == FundingStatus.OPEN) { "모집중인 라운드가 아닙니다 (status=$status)" }
        require(amount.isPositive()) { "투자 금액은 0보다 커야 합니다: $amount" }
        require(amount <= remaining) {
            "모집 한도 초과 — 잔여=$remaining, 요청=$amount"
        }

        raised += amount
        val events = mutableListOf<Any>(InvestmentMade(id, investorId, amount))
        if (raised == targetAmount) {
            status = FundingStatus.FULFILLED
            events += FundingFulfilled(id, raised)
        }
        return events
    }
}
