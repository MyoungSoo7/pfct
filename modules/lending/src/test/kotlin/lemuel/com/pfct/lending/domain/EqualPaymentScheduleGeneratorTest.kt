package lemuel.com.pfct.lending.domain

import lemuel.com.pfct.common.AnnualInterestRate
import lemuel.com.pfct.common.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EqualPaymentScheduleGeneratorTest {

    @Test
    fun `회차 수가 개월 수와 같다`() {
        val schedule = EqualPaymentScheduleGenerator.generate(
            principal = Money.won(12_000_000),
            annualRate = AnnualInterestRate.of("12.0"),
            months = 12,
        )
        assertEquals(12, schedule.size)
        assertEquals((1..12).toList(), schedule.map { it.sequence })
    }

    @Test
    fun `원금 합계가 대출 원금과 정확히 일치한다 — 1원도 어긋나지 않는다`() {
        val principal = Money.won(10_000_000)
        val schedule = EqualPaymentScheduleGenerator.generate(
            principal = principal,
            annualRate = AnnualInterestRate.of("15.5"),
            months = 24,
        )
        val principalSum = schedule.fold(Money.ZERO) { acc, i -> acc + i.principal }
        assertEquals(principal, principalSum)
    }

    @Test
    fun `매 회차 이자는 남은 원금에 비례해 점점 줄어든다`() {
        val schedule = EqualPaymentScheduleGenerator.generate(
            principal = Money.won(6_000_000),
            annualRate = AnnualInterestRate.of("18.0"),
            months = 6,
        )
        // 원리금균등에서는 이자가 단조 감소해야 한다.
        val interests = schedule.map { it.interest }
        for (i in 1 until interests.size) {
            assertTrue(interests[i] <= interests[i - 1], "이자가 증가한 회차 발견: $i")
        }
    }

    @Test
    fun `무이자 대출은 원금을 균등 분할하고 이자는 0 이다`() {
        val schedule = EqualPaymentScheduleGenerator.generate(
            principal = Money.won(1_200_000),
            annualRate = AnnualInterestRate.of("0.0"),
            months = 12,
        )
        assertTrue(schedule.all { it.interest.isZero() })
        val principalSum = schedule.fold(Money.ZERO) { acc, i -> acc + i.principal }
        assertEquals(Money.won(1_200_000), principalSum)
    }

    @Test
    fun `첫 회차 이자는 원금 곱하기 월이자율을 반올림한 값이다`() {
        // 원금 1,000,000 / 연 12% → 월이자율 0.01 → 첫 달 이자 10,000원
        val schedule = EqualPaymentScheduleGenerator.generate(
            principal = Money.won(1_000_000),
            annualRate = AnnualInterestRate.of("12.0"),
            months = 12,
        )
        assertEquals(Money.won(10_000), schedule.first().interest)
    }
}
