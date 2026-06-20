package lemuel.com.pfct.lending.domain

import lemuel.com.pfct.common.AnnualInterestRate
import lemuel.com.pfct.common.Money
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class RepaymentStatus { DUE, PAID, OVERDUE }

/**
 * 상태를 가진 상환 회차(영속화 대상). 대출 실행 시 스케줄로 생성되고, 정산되면 PAID,
 * 납기를 넘기면 OVERDUE 로 전이된다. 상태 전이 규칙을 도메인 안에 가둔다.
 */
class ScheduledRepayment(
    val loanId: LoanId,
    val sequence: Int,
    val dueDate: LocalDate,
    val principal: Money,
    val interest: Money,
    status: RepaymentStatus = RepaymentStatus.DUE,
    lateFee: Money = Money.ZERO,
) {
    var status: RepaymentStatus = status
        private set
    var lateFee: Money = lateFee
        private set

    /** 해당 회차 납입 총액(원리금). */
    val total: Money get() = principal + interest

    /** 정산(상환) 완료 처리. 연체 상태였더라도 납입되면 PAID 로 정리된다(멱등). */
    fun markPaid() {
        status = RepaymentStatus.PAID
    }

    /**
     * 연체 전이. DUE 이고 납기가 지났을 때만 OVERDUE 로 바꾸고 연체료를 계산한다.
     * @return 이번 호출로 실제 연체 전이가 일어났으면 true(이미 PAID/OVERDUE 거나 미연체면 false → 멱등)
     */
    fun markOverdue(asOf: LocalDate, delinquencyRate: AnnualInterestRate): Boolean {
        if (status != RepaymentStatus.DUE) return false
        if (!dueDate.isBefore(asOf)) return false

        val daysOverdue = ChronoUnit.DAYS.between(dueDate, asOf)
        lateFee = DelinquencyCalculator.lateFee(total, delinquencyRate, daysOverdue)
        status = RepaymentStatus.OVERDUE
        return true
    }
}
