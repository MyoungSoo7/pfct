package lemuel.com.pfct.lending.application

import lemuel.com.pfct.common.AnnualInterestRate
import lemuel.com.pfct.common.Money
import lemuel.com.pfct.lending.domain.BorrowerId
import lemuel.com.pfct.lending.domain.Loan
import lemuel.com.pfct.lending.domain.LoanId
import lemuel.com.pfct.lending.domain.ScheduledRepayment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 대출 생성 Saga 단계.
 * - [create]: 대출을 생성·저장하고 **상환 스케줄(회차별 납기)** 도 함께 영속화(멱등 — 같은 ID면 no-op).
 * - [cancel]: 후속 단계 실패 시 대출을 취소(보상 트랜잭션).
 */
@Service
class CreateLoanService(
    private val repository: LoanRepository,
    private val repayments: LoanRepaymentRepository,
) {
    @Transactional
    fun create(command: CreateLoanCommand): LoanId {
        val id = LoanId(command.loanId)
        if (repository.existsById(id)) return id // 멱등: 재실행 안전

        val loan = Loan(
            id = id,
            borrowerId = BorrowerId(command.borrowerId),
            principal = command.principal,
            annualRate = command.annualRate,
            months = command.months,
        )
        repository.save(loan)

        // 상환 스케줄 생성: 회차 N의 납기 = 기준일 + N개월.
        val schedule = loan.repaymentSchedule().map { installment ->
            ScheduledRepayment(
                loanId = id,
                sequence = installment.sequence,
                dueDate = command.startDate.plusMonths(installment.sequence.toLong()),
                principal = installment.principal,
                interest = installment.interest,
            )
        }
        repayments.saveAll(schedule)
        return id
    }

    @Transactional
    fun cancel(loanId: LoanId) {
        val loan = repository.findById(loanId) ?: return
        loan.cancel()
        repository.save(loan)
    }
}

data class CreateLoanCommand(
    val loanId: String,
    val borrowerId: String,
    val principal: Money,
    val annualRate: AnnualInterestRate,
    val months: Int,
    val startDate: LocalDate = LocalDate.now(),
)
