package lemuel.com.pfct.investment.domain

import lemuel.com.pfct.common.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FundingRoundTest {

    private fun newRound(target: Long = 1_000_000) =
        FundingRound(FundingRoundId("round-1"), Money.won(target))

    @Test
    fun `투자가 누적되고 잔여 모집액이 줄어든다`() {
        val round = newRound()
        round.invest(InvestorId("a"), Money.won(300_000))
        assertEquals(Money.won(300_000), round.raised)
        assertEquals(Money.won(700_000), round.remaining)
        assertEquals(FundingStatus.OPEN, round.status)
    }

    @Test
    fun `목표를 정확히 채우면 FULFILLED 가 되고 FundingFulfilled 이벤트가 발생한다`() {
        val round = newRound()
        round.invest(InvestorId("a"), Money.won(600_000))
        val events = round.invest(InvestorId("b"), Money.won(400_000))

        assertEquals(FundingStatus.FULFILLED, round.status)
        assertTrue(events.any { it is InvestmentMade })
        assertTrue(events.any { it is FundingFulfilled })
    }

    @Test
    fun `잔여 모집액을 초과하는 투자는 거부된다 — 오버펀딩 금지`() {
        val round = newRound()
        round.invest(InvestorId("a"), Money.won(900_000))
        val ex = assertFailsWith<IllegalArgumentException> {
            round.invest(InvestorId("b"), Money.won(200_000))
        }
        assertTrue(ex.message!!.contains("모집 한도 초과"))
        assertEquals(Money.won(900_000), round.raised) // 상태 변화 없음
    }

    @Test
    fun `모집 완료된 라운드에는 더 이상 투자할 수 없다`() {
        val round = newRound()
        round.invest(InvestorId("a"), Money.won(1_000_000))
        assertFailsWith<IllegalStateException> {
            round.invest(InvestorId("b"), Money.won(1))
        }
    }
}
