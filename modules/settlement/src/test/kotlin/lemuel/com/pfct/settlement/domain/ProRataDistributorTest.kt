package lemuel.com.pfct.settlement.domain

import lemuel.com.pfct.common.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProRataDistributorTest {

    private fun sum(parts: List<Money>) = parts.fold(Money.ZERO) { acc, m -> acc + m }

    @Test
    fun `비율대로 정확히 나뉘고 합계가 원금과 같다`() {
        val parts = ProRataDistributor.distribute(
            total = Money.won(1_000_000),
            weights = listOf(Money.won(600_000), Money.won(400_000)),
        )
        assertEquals(listOf(Money.won(600_000), Money.won(400_000)), parts)
        assertEquals(Money.won(1_000_000), sum(parts))
    }

    @Test
    fun `나누어떨어지지 않아도 합계는 1원도 어긋나지 않는다`() {
        // 10,000원을 3등분: 3334 + 3333 + 3333 = 10,000
        val parts = ProRataDistributor.distribute(
            total = Money.won(10_000),
            weights = listOf(Money.won(1), Money.won(1), Money.won(1)),
        )
        assertEquals(Money.won(10_000), sum(parts))
        // 최대 잉여 방식: 첫 번째가 1원 더 받는다.
        assertEquals(listOf(Money.won(3_334), Money.won(3_333), Money.won(3_333)), parts)
    }

    @Test
    fun `단수는 잉여가 큰 쪽에 배분된다`() {
        // 100원을 7:2:1 비율 → 70, 20, 10 (정확히 나뉨)
        val parts = ProRataDistributor.distribute(
            total = Money.won(100),
            weights = listOf(Money.won(70), Money.won(20), Money.won(10)),
        )
        assertEquals(listOf(Money.won(70), Money.won(20), Money.won(10)), parts)
    }

    @Test
    fun `투자자가 한 명이면 전액을 받는다`() {
        val parts = ProRataDistributor.distribute(Money.won(987_654), listOf(Money.won(5)))
        assertEquals(listOf(Money.won(987_654)), parts)
    }

    @Test
    fun `여러 단수가 있어도 합계가 보존된다`() {
        // 10원을 7명에게 균등 분배 → 합 10
        val parts = ProRataDistributor.distribute(
            total = Money.won(10),
            weights = List(7) { Money.won(1) },
        )
        assertEquals(Money.won(10), sum(parts))
        assertTrue(parts.all { it.amount.toLong() in 1..2 })
    }

    @Test
    fun `가중치 합이 0이면 예외`() {
        assertFailsWith<IllegalArgumentException> {
            ProRataDistributor.distribute(Money.won(100), listOf(Money.ZERO, Money.ZERO))
        }
    }
}
