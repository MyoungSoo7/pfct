package lemuel.com.pfct.lending.domain

import lemuel.com.pfct.common.AnnualInterestRate
import lemuel.com.pfct.common.Money

enum class LoanStatus { ACTIVE, CANCELLED }

/**
 * 대출(Aggregate Root). 모집 완료된 펀딩 라운드를 실행하면 생성된다.
 * 상환 스케줄은 상태로 들고 있지 않고, 원금·이자율·기간으로부터 [repaymentSchedule]로 도출한다.
 */
class Loan(
    val id: LoanId,
    val borrowerId: BorrowerId,
    val principal: Money,
    val annualRate: AnnualInterestRate,
    val months: Int,
    status: LoanStatus = LoanStatus.ACTIVE,
) {
    var status: LoanStatus = status
        private set

    init {
        require(principal.isPositive()) { "대출 원금은 0보다 커야 합니다: $principal" }
        require(months >= 1) { "대출 기간(개월)은 1 이상이어야 합니다: $months" }
    }

    /** 원리금균등 상환 스케줄을 도출한다. */
    fun repaymentSchedule(): List<RepaymentInstallment> =
        EqualPaymentScheduleGenerator.generate(principal, annualRate, months)

    /** 대출을 취소한다(Saga 보상 트랜잭션). */
    fun cancel() {
        status = LoanStatus.CANCELLED
    }
}
