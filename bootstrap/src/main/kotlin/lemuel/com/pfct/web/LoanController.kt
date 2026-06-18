package lemuel.com.pfct.web

import lemuel.com.pfct.saga.ExecuteLoanCommand
import lemuel.com.pfct.saga.LoanExecutionSaga
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/loans")
class LoanController(
    private val loanExecutionSaga: LoanExecutionSaga,
) {
    /** 모집 완료된 라운드를 실행해 대출을 일으키고 차주에게 지급한다(Saga). */
    @PostMapping("/execute")
    fun execute(@RequestBody request: ExecuteLoanRequest): ExecuteLoanResponse {
        val result = loanExecutionSaga.execute(
            ExecuteLoanCommand(
                roundId = request.roundId,
                loanId = request.loanId,
                borrowerId = request.borrowerId,
                annualRatePercent = request.annualRatePercent,
                months = request.months,
            ),
        )
        return ExecuteLoanResponse(
            loanId = result.loanId,
            principal = result.principal.amount.longValueExact(),
        )
    }
}

data class ExecuteLoanRequest(
    val roundId: String,
    val loanId: String,
    val borrowerId: String,
    val annualRatePercent: BigDecimal,
    val months: Int,
)

data class ExecuteLoanResponse(
    val loanId: String,
    val principal: Long,
)
