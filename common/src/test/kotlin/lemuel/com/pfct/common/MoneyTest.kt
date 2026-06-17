package lemuel.com.pfct.common

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MoneyTest {

    @Test
    fun `원 단위 정수로 생성하고 더하기 빼기가 정확하다`() {
        val sum = Money.won(1_000) + Money.won(2_500)
        assertEquals(Money.won(3_500), sum)
        assertEquals(Money.won(500), Money.won(1_000) - Money.won(500))
    }

    @Test
    fun `원 미만 소수가 있는 BigDecimal 은 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            Money.won(BigDecimal("100.5"))
        }
    }

    @Test
    fun `소수점이 0 으로만 끝나는 값은 허용한다`() {
        assertEquals(Money.won(1_000), Money.won(BigDecimal("1000.00")))
    }

    @Test
    fun `부호 판별이 정확하다`() {
        assertTrue(Money.won(1).isPositive())
        assertTrue(Money.ZERO.isZero())
        assertTrue((Money.won(1) - Money.won(2)).isNegative())
    }
}
