package lemuel.com.pfct.lending.domain

import lemuel.com.pfct.common.AnnualInterestRate
import lemuel.com.pfct.common.Money
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * 원리금균등상환(Equal Payment) 스케줄 생성기.
 *
 * 매월 상환액(원금+이자)이 일정한 방식. 월 상환액 A 는 다음 공식으로 구한다:
 *
 *     A = P · r · (1+r)^n / ((1+r)^n − 1)
 *
 *   P = 원금, r = 월이자율, n = 총 개월수
 *
 * 구현상의 정합성 포인트:
 * 1. 모든 계산은 [BigDecimal] + 명시적 [RoundingMode] 로 수행 (부동소수점 오차 0).
 * 2. 매 회차 이자는 "남은 원금 × 월이자율"을 원 단위로 반올림.
 * 3. **마지막 회차 원금 = 남은 원금 전액**으로 보정 → 원금 합계가 대출 원금과 1원도 어긋나지 않음.
 */
object EqualPaymentScheduleGenerator {

    private val MC = MathContext(20, RoundingMode.HALF_UP)

    fun generate(
        principal: Money,
        annualRate: AnnualInterestRate,
        months: Int,
    ): List<RepaymentInstallment> {
        require(months >= 1) { "상환 개월수는 1 이상이어야 합니다: $months" }
        require(principal.isPositive()) { "대출 원금은 0보다 커야 합니다: $principal" }

        val monthlyRate = annualRate.monthlyRate()
        return if (monthlyRate.signum() == 0) {
            generateInterestFree(principal, months)
        } else {
            generateWithInterest(principal, monthlyRate, months)
        }
    }

    private fun generateWithInterest(
        principal: Money,
        monthlyRate: BigDecimal,
        months: Int,
    ): List<RepaymentInstallment> {
        val principalAmount = principal.amount
        val onePlusR = BigDecimal.ONE.add(monthlyRate)
        val pow = onePlusR.pow(months, MC)
        // 월 상환액 A (반올림 전 원본)
        val monthlyPayment = principalAmount
            .multiply(monthlyRate, MC)
            .multiply(pow, MC)
            .divide(pow.subtract(BigDecimal.ONE), MC)

        val installments = ArrayList<RepaymentInstallment>(months)
        var remaining = principalAmount // 남은 원금 (원 단위 정수 유지)

        for (seq in 1..months) {
            val interestWon = remaining.multiply(monthlyRate, MC).setScale(0, RoundingMode.HALF_UP)
            val principalWon: BigDecimal = if (seq == months) {
                remaining // 마지막 회차: 남은 원금 전액으로 잔액 정확히 0 만들기
            } else {
                monthlyPayment.setScale(0, RoundingMode.HALF_UP).subtract(interestWon)
            }
            remaining = remaining.subtract(principalWon)
            installments += RepaymentInstallment(seq, Money.won(principalWon), Money.won(interestWon))
        }
        return installments
    }

    /** 무이자 대출: 원금을 균등 분할하고, 단수(나머지)는 마지막 회차에 몰아준다. */
    private fun generateInterestFree(principal: Money, months: Int): List<RepaymentInstallment> {
        val principalAmount = principal.amount
        val base = principalAmount.divide(BigDecimal.valueOf(months.toLong()), 0, RoundingMode.DOWN)
        val installments = ArrayList<RepaymentInstallment>(months)
        var remaining = principalAmount
        for (seq in 1..months) {
            val principalWon = if (seq == months) remaining else base
            remaining = remaining.subtract(principalWon)
            installments += RepaymentInstallment(seq, Money.won(principalWon), Money.ZERO)
        }
        return installments
    }
}
