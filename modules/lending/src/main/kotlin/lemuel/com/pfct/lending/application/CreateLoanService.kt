package lemuel.com.pfct.lending.application

import lemuel.com.pfct.common.AnnualInterestRate
import lemuel.com.pfct.common.Money
import lemuel.com.pfct.lending.domain.BorrowerId
import lemuel.com.pfct.lending.domain.Loan
import lemuel.com.pfct.lending.domain.LoanId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 대출 생성 Saga 단계.
 * - [create]: 대출을 생성·저장(멱등 — 같은 ID가 이미 있으면 no-op).
 * - [cancel]: 후속 단계 실패 시 대출을 취소(보상 트랜잭션).
 */
@Service
class CreateLoanService(
    private val repository: LoanRepository,
) {
    @Transactional
    fun create(command: CreateLoanCommand): LoanId {
        val id = LoanId(command.loanId)
        if (repository.existsById(id)) return id // 멱등: 재실행 안전

        repository.save(
            Loan(
                id = id,
                borrowerId = BorrowerId(command.borrowerId),
                principal = command.principal,
                annualRate = command.annualRate,
                months = command.months,
            ),
        )
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
)
