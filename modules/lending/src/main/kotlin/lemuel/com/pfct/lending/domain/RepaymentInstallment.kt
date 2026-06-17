package lemuel.com.pfct.lending.domain

import lemuel.com.pfct.common.Money

/**
 * 상환 회차 1건. 매 회차는 원금(principal)과 이자(interest)로 분리된다.
 * 정산 시 투자자에게 분배할 금액과 플랫폼 이자 수익을 구분하기 위해 이 분리가 필수다.
 */
data class RepaymentInstallment(
    val sequence: Int,
    val principal: Money,
    val interest: Money,
) {
    init {
        require(sequence >= 1) { "회차는 1 이상이어야 합니다: $sequence" }
        require(!principal.isNegative()) { "원금은 음수일 수 없습니다: $principal" }
        require(!interest.isNegative()) { "이자는 음수일 수 없습니다: $interest" }
    }

    /** 해당 회차에 차주가 납입하는 총액(원리금). */
    val total: Money get() = principal + interest
}
