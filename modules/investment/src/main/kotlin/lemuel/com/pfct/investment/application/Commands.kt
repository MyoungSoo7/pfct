package lemuel.com.pfct.investment.application

import lemuel.com.pfct.common.Money
import lemuel.com.pfct.investment.domain.FundingRoundId
import lemuel.com.pfct.investment.domain.FundingStatus
import lemuel.com.pfct.investment.domain.InvestorId

data class OpenFundingRoundCommand(
    val id: String,
    val targetAmount: Money,
)

data class InvestCommand(
    val roundId: FundingRoundId,
    val investorId: InvestorId,
    val amount: Money,
)

data class InvestResult(
    val roundId: FundingRoundId,
    val raised: Money,
    val remaining: Money,
    val status: FundingStatus,
    val events: List<Any>,
)

class FundingRoundNotFoundException(id: FundingRoundId) :
    RuntimeException("펀딩 라운드를 찾을 수 없습니다: ${id.value}")
