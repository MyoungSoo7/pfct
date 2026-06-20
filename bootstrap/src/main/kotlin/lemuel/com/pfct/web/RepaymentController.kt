package lemuel.com.pfct.web

import lemuel.com.pfct.lending.application.LoanRepaymentRepository
import lemuel.com.pfct.lending.domain.LoanId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 대출의 상환 스케줄/연체 상태 조회. */
@RestController
@RequestMapping("/api/loans/{loanId}/repayments")
class RepaymentController(
    private val repayments: LoanRepaymentRepository,
) {
    @GetMapping
    fun list(@PathVariable loanId: String): List<RepaymentView> =
        repayments.findByLoanId(LoanId(loanId)).map { entry ->
            RepaymentView(
                sequence = entry.sequence,
                dueDate = entry.dueDate.toString(),
                principal = entry.principal.amount.longValueExact(),
                interest = entry.interest.amount.longValueExact(),
                lateFee = entry.lateFee.amount.longValueExact(),
                status = entry.status.name,
            )
        }
}

data class RepaymentView(
    val sequence: Int,
    val dueDate: String,
    val principal: Long,
    val interest: Long,
    val lateFee: Long,
    val status: String,
)
