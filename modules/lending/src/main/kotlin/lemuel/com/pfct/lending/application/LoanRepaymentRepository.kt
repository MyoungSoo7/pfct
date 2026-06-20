package lemuel.com.pfct.lending.application

import lemuel.com.pfct.lending.domain.LoanId
import lemuel.com.pfct.lending.domain.ScheduledRepayment
import java.time.LocalDate

/** 상태 있는 상환 회차 영속화 포트. */
interface LoanRepaymentRepository {

    fun saveAll(entries: List<ScheduledRepayment>)

    /** 기존 회차의 상태/연체료 변경을 반영한다(loan_id + sequence 로 식별). */
    fun update(entry: ScheduledRepayment)

    fun findByLoanIdAndSequence(loanId: LoanId, sequence: Int): ScheduledRepayment?

    fun findByLoanId(loanId: LoanId): List<ScheduledRepayment>

    /** 납기가 지났는데 아직 DUE(미상환) 인 회차 = 연체 후보. */
    fun findOverdueCandidates(asOf: LocalDate): List<ScheduledRepayment>
}
