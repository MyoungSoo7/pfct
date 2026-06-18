package lemuel.com.pfct.investment.application

import lemuel.com.pfct.common.Money
import lemuel.com.pfct.investment.domain.FundingRoundId
import lemuel.com.pfct.investment.domain.InvestorId

/**
 * 개별 투자 내역 포트. 정산 시 투자자별 분배 비율을 계산하려면 각 투자자의 기여액이 필요하다.
 */
interface InvestmentRepository {

    fun save(roundId: FundingRoundId, investorId: InvestorId, amount: Money)

    /** 라운드의 투자자별 누적 투자액(한 투자자가 여러 번 투자했으면 합산). */
    fun findSharesByRound(roundId: FundingRoundId): List<InvestorShare>
}

data class InvestorShare(
    val investorId: InvestorId,
    val amount: Money,
)
