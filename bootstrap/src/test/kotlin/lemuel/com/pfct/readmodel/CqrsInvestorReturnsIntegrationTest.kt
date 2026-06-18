package lemuel.com.pfct.readmodel

import com.fasterxml.jackson.databind.ObjectMapper
import lemuel.com.pfct.AbstractIntegrationTest
import lemuel.com.pfct.common.Money
import lemuel.com.pfct.event.InvestorDistribution
import lemuel.com.pfct.event.RepaymentSettledEvent
import lemuel.com.pfct.investment.application.InvestCommand
import lemuel.com.pfct.investment.application.InvestService
import lemuel.com.pfct.investment.application.OpenFundingRoundCommand
import lemuel.com.pfct.investment.application.OpenFundingRoundService
import lemuel.com.pfct.investment.domain.FundingRoundId
import lemuel.com.pfct.investment.domain.InvestorId
import lemuel.com.pfct.saga.ExecuteLoanCommand
import lemuel.com.pfct.saga.LoanExecutionSaga
import lemuel.com.pfct.settlement.SettleRepaymentCommand
import lemuel.com.pfct.settlement.SettleRepaymentService
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CqrsInvestorReturnsIntegrationTest @Autowired constructor(
    private val openRound: OpenFundingRoundService,
    private val invest: InvestService,
    private val saga: LoanExecutionSaga,
    private val settle: SettleRepaymentService,
    private val views: InvestorReturnViewRepository,
    private val projector: InvestorReturnsProjector,
    private val objectMapper: ObjectMapper,
) : AbstractIntegrationTest() {

    @Test
    fun `정산하면 투자자 수익 읽기 모델이 이벤트로 갱신된다`() {
        val roundId = "cqrs-round"
        val loanId = "cqrs-loan"
        openRound.open(OpenFundingRoundCommand(roundId, Money.won(1_000_000)))
        invest.invest(InvestCommand(FundingRoundId(roundId), InvestorId("cqrs-A"), Money.won(600_000)))
        invest.invest(InvestCommand(FundingRoundId(roundId), InvestorId("cqrs-B"), Money.won(400_000)))
        saga.execute(ExecuteLoanCommand(roundId, loanId, "cqrs-borrower", BigDecimal("12.0"), 12))

        settle.settle(SettleRepaymentCommand(loanId, roundId, sequence = 1, feeRatePercent = BigDecimal("10")))

        // 아웃박스 → Kafka → 프로젝터를 거쳐 읽기 모델이 비동기로 갱신된다.
        await().atMost(20, TimeUnit.SECONDS).until {
            views.findById("cqrs-A").isPresent && views.findById("cqrs-B").isPresent
        }

        val a = views.findById("cqrs-A").get()
        val b = views.findById("cqrs-B").get()
        assertTrue(a.totalReturned > 0 && b.totalReturned > 0)
        assertTrue(a.totalReturned > b.totalReturned, "6:4 비율이면 A 수익이 더 커야 한다")
        assertEquals(1, a.settlementCount)
    }

    @Test
    fun `같은 정산 이벤트를 두 번 처리해도 한 번만 반영된다 — 읽기측 멱등`() {
        val event = RepaymentSettledEvent(
            settlementId = "settle:idem-cqrs-loan:1",
            loanId = "idem-cqrs-loan",
            distributions = listOf(InvestorDistribution("idem-investor", 50_000)),
        )
        val payload = objectMapper.writeValueAsString(event)

        projector.on(payload, "RepaymentSettled")
        projector.on(payload, "RepaymentSettled") // 재전달 시뮬레이션

        val view = views.findById("idem-investor").get()
        assertEquals(50_000L, view.totalReturned, "중복 처리되어도 한 번만 반영")
        assertEquals(1, view.settlementCount)
    }
}
