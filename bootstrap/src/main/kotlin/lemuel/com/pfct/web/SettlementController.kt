package lemuel.com.pfct.web

import lemuel.com.pfct.settlement.SettleRepaymentCommand
import lemuel.com.pfct.settlement.SettleRepaymentService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/loans/{loanId}/settlements")
class SettlementController(
    private val settleRepayment: SettleRepaymentService,
) {
    /** 차주의 한 회차 상환금을 투자자에게 비율대로 정산한다. */
    @PostMapping
    fun settle(
        @PathVariable loanId: String,
        @RequestBody request: SettleRequest,
    ): SettleResponse {
        val result = settleRepayment.settle(
            SettleRepaymentCommand(
                loanId = loanId,
                roundId = request.roundId,
                sequence = request.sequence,
                feeRatePercent = request.feeRatePercent,
            ),
        )
        return SettleResponse(
            loanId = result.loanId,
            sequence = result.sequence,
            fee = result.fee.amount.longValueExact(),
            distributions = result.distributions.map { (investorId, amount) ->
                DistributionView(investorId, amount.amount.longValueExact())
            },
            applied = result.applied,
        )
    }
}

data class SettleRequest(
    val roundId: String,
    val sequence: Int,
    val feeRatePercent: BigDecimal,
)

data class SettleResponse(
    val loanId: String,
    val sequence: Int,
    val fee: Long,
    val distributions: List<DistributionView>,
    val applied: Boolean,
)

data class DistributionView(val investorId: String, val amount: Long)
