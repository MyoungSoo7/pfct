package lemuel.com.pfct.lending.domain

import lemuel.com.pfct.common.AnnualInterestRate
import lemuel.com.pfct.common.Money
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DelinquencyTest {

    private fun rate(p: String) = AnnualInterestRate.of(p)

    @Test
    fun `연체료 = 원리금 x 연체이자율 x 일수 div 365, 원 단위 절사`() {
        // 1,000,000 × 12% × 30/365 = 9863.01... → 9863
        assertEquals(Money.won(9_863), DelinquencyCalculator.lateFee(Money.won(1_000_000), rate("12.0"), 30))
    }

    @Test
    fun `연체일수가 0 이하면 연체료는 0`() {
        assertEquals(Money.ZERO, DelinquencyCalculator.lateFee(Money.won(1_000_000), rate("12.0"), 0))
    }

    private fun entry(due: LocalDate) = ScheduledRepayment(
        loanId = LoanId("loan-1"),
        sequence = 1,
        dueDate = due,
        principal = Money.won(90_000),
        interest = Money.won(10_000),
    )

    @Test
    fun `납기가 지난 DUE 회차는 연체로 전이되고 연체료가 매겨진다`() {
        val e = entry(LocalDate.of(2026, 1, 1))
        val transitioned = e.markOverdue(LocalDate.of(2026, 1, 31), rate("12.0")) // 30일 연체

        assertTrue(transitioned)
        assertEquals(RepaymentStatus.OVERDUE, e.status)
        assertTrue(e.lateFee.isPositive())
    }

    @Test
    fun `이미 연체된 회차의 재처리는 멱등하다`() {
        val e = entry(LocalDate.of(2026, 1, 1))
        e.markOverdue(LocalDate.of(2026, 1, 31), rate("12.0"))
        val second = e.markOverdue(LocalDate.of(2026, 2, 28), rate("12.0"))
        assertFalse(second, "이미 OVERDUE 면 다시 전이되지 않는다")
    }

    @Test
    fun `납기 전이면 연체되지 않는다`() {
        val e = entry(LocalDate.of(2026, 12, 31))
        assertFalse(e.markOverdue(LocalDate.of(2026, 6, 1), rate("12.0")))
        assertEquals(RepaymentStatus.DUE, e.status)
    }

    @Test
    fun `납입 완료된 회차는 연체되지 않는다`() {
        val e = entry(LocalDate.of(2026, 1, 1))
        e.markPaid()
        assertFalse(e.markOverdue(LocalDate.of(2026, 6, 1), rate("12.0")))
        assertEquals(RepaymentStatus.PAID, e.status)
    }
}
