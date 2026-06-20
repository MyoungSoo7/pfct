package lemuel.com.pfct.overdue

import com.fasterxml.jackson.databind.ObjectMapper
import lemuel.com.pfct.event.RepaymentOverdueEvent
import lemuel.com.pfct.lending.application.LoanRepaymentRepository
import lemuel.com.pfct.lending.application.LoanRepository
import lemuel.com.pfct.lending.domain.LoanId
import lemuel.com.pfct.outbox.OutboxRecorder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 한 상환 회차의 연체 전이를 처리한다. **상태 변경과 아웃박스 이벤트 적재를 같은 트랜잭션**으로 묶어
 * (전환됐는데 이벤트 유실 같은) 불일치를 막는다(트랜잭셔널 아웃박스).
 *
 * 연체이자율은 대출의 약정 이자율을 사용한다. 멱등: 이미 OVERDUE/PAID 면 아무 일도 하지 않는다.
 */
@Service
class OverdueProcessingService(
    private val loans: LoanRepository,
    private val repayments: LoanRepaymentRepository,
    private val outbox: OutboxRecorder,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun process(loanId: LoanId, sequence: Int, asOf: LocalDate): Boolean {
        val loan = loans.findById(loanId) ?: return false
        val entry = repayments.findByLoanIdAndSequence(loanId, sequence) ?: return false

        val transitioned = entry.markOverdue(asOf, loan.annualRate)
        if (!transitioned) return false

        repayments.update(entry)
        val event = RepaymentOverdueEvent(
            loanId = loanId.value,
            sequence = sequence,
            overdueAmount = entry.total.amount.longValueExact(),
            lateFee = entry.lateFee.amount.longValueExact(),
            dueDate = entry.dueDate.toString(),
        )
        outbox.record("Loan", loanId.value, "RepaymentOverdue", objectMapper.writeValueAsString(event))
        return true
    }
}
