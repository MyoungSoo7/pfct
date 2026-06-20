package lemuel.com.pfct.lending.domain

import lemuel.com.pfct.common.AnnualInterestRate
import lemuel.com.pfct.common.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 연체료(지연배상금) 계산기 — 순수 도메인.
 *
 * 연체료 = 연체 원리금 × 연체이자율(연) × 연체일수 / 365, **원 단위 절사(내림)**.
 * 모든 계산은 BigDecimal 정수 연산으로 수행해 오차가 없다.
 */
object DelinquencyCalculator {

    // 100(%) × 365(일) = 36500. (overdue × percent × days) / 36500 = overdue × (percent/100) × (days/365)
    private val PERCENT_TIMES_YEAR = BigDecimal(36_500)

    fun lateFee(overdueAmount: Money, delinquencyRate: AnnualInterestRate, daysOverdue: Long): Money {
        if (daysOverdue <= 0) return Money.ZERO
        val numerator = overdueAmount.amount
            .multiply(delinquencyRate.percent)
            .multiply(BigDecimal(daysOverdue))
        val fee = numerator.divide(PERCENT_TIMES_YEAR, 0, RoundingMode.DOWN)
        return Money.won(fee)
    }
}
