package lemuel.com.pfct.web

import lemuel.com.pfct.common.Money
import lemuel.com.pfct.investment.application.InvestCommand
import lemuel.com.pfct.investment.application.InvestService
import lemuel.com.pfct.investment.application.OpenFundingRoundCommand
import lemuel.com.pfct.investment.application.OpenFundingRoundService
import lemuel.com.pfct.investment.domain.FundingRoundId
import lemuel.com.pfct.investment.domain.InvestorId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/funding-rounds")
class FundingRoundController(
    private val openFundingRoundService: OpenFundingRoundService,
    private val investService: InvestService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun open(@RequestBody request: OpenRoundRequest): OpenRoundResponse {
        val id = openFundingRoundService.open(
            OpenFundingRoundCommand(request.id, Money.won(request.targetAmount)),
        )
        return OpenRoundResponse(id.value)
    }

    @PostMapping("/{roundId}/investments")
    fun invest(
        @PathVariable roundId: String,
        @RequestBody request: InvestRequest,
    ): InvestResponse {
        val result = investService.invest(
            InvestCommand(
                roundId = FundingRoundId(roundId),
                investorId = InvestorId(request.investorId),
                amount = Money.won(request.amount),
            ),
        )
        return InvestResponse(
            roundId = result.roundId.value,
            raised = result.raised.amount.longValueExact(),
            remaining = result.remaining.amount.longValueExact(),
            status = result.status.name,
        )
    }
}

data class OpenRoundRequest(val id: String, val targetAmount: Long)
data class OpenRoundResponse(val roundId: String)

data class InvestRequest(val investorId: String, val amount: Long)
data class InvestResponse(
    val roundId: String,
    val raised: Long,
    val remaining: Long,
    val status: String,
)
