package lemuel.com.pfct.saga

import lemuel.com.pfct.AbstractIntegrationTest
import lemuel.com.pfct.common.Money
import lemuel.com.pfct.investment.adapter.persistence.FundingRoundJpaRepository
import lemuel.com.pfct.investment.application.InvestCommand
import lemuel.com.pfct.investment.application.InvestService
import lemuel.com.pfct.investment.application.OpenFundingRoundCommand
import lemuel.com.pfct.investment.application.OpenFundingRoundService
import lemuel.com.pfct.investment.domain.FundingRoundId
import lemuel.com.pfct.investment.domain.FundingStatus
import lemuel.com.pfct.investment.domain.InvestorId
import lemuel.com.pfct.ledger.application.LedgerRepository
import lemuel.com.pfct.ledger.domain.AccountId
import lemuel.com.pfct.lending.adapter.persistence.LoanJpaRepository
import lemuel.com.pfct.lending.domain.LoanStatus
import lemuel.com.pfct.outbox.OutboxJpaRepository
import lemuel.com.pfct.outbox.OutboxRelay
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoanExecutionSagaIntegrationTest @Autowired constructor(
    private val openRound: OpenFundingRoundService,
    private val invest: InvestService,
    private val saga: LoanExecutionSaga,
    private val rounds: FundingRoundJpaRepository,
    private val loans: LoanJpaRepository,
    private val ledger: LedgerRepository,
    private val outbox: OutboxJpaRepository,
    private val relay: OutboxRelay,
) : AbstractIntegrationTest() {

    private fun fundFully(roundId: String, principal: Long) {
        openRound.open(OpenFundingRoundCommand(roundId, Money.won(principal)))
        invest.invest(InvestCommand(FundingRoundId(roundId), InvestorId("inv-$roundId"), Money.won(principal)))
    }

    @Test
    fun `정상 실행 — 라운드 EXECUTED, 대출 생성, 차주 지급, 아웃박스 이벤트 적재`() {
        val roundId = "round-happy"
        val loanId = "loan-happy"
        val borrowerId = "borrower-happy"
        val principal = 1_000_000L
        fundFully(roundId, principal)

        val result = saga.execute(ExecuteLoanCommand(roundId, loanId, borrowerId, BigDecimal("12.0"), 12))

        assertEquals(Money.won(principal), result.principal)
        assertEquals(FundingStatus.EXECUTED, rounds.findById(roundId).orElseThrow().status)
        assertEquals(LoanStatus.ACTIVE, loans.findById(loanId).orElseThrow().status)
        assertEquals(Money.won(principal), ledger.balanceOf(AccountId("borrower:$borrowerId")))
        assertEquals(1L, outbox.countByAggregateId(loanId), "LoanDisbursed 이벤트가 한 건 적재되어야 한다")

        // 릴레이가 미발행 이벤트를 발행 처리한다.
        assertTrue(relay.publishPending() >= 1)
    }

    @Test
    fun `Saga 재실행은 멱등하다 — 두 번 실행해도 잔액과 이벤트가 중복되지 않는다`() {
        val roundId = "round-idem"
        val loanId = "loan-idem"
        val borrowerId = "borrower-idem"
        val principal = 2_000_000L
        fundFully(roundId, principal)

        val command = ExecuteLoanCommand(roundId, loanId, borrowerId, BigDecimal("10.0"), 24)
        saga.execute(command)
        saga.execute(command) // 재실행 — 예외 없이 멱등 처리되어야 한다

        assertEquals(FundingStatus.EXECUTED, rounds.findById(roundId).orElseThrow().status)
        assertEquals(Money.won(principal), ledger.balanceOf(AccountId("borrower:$borrowerId"))) // 2배 아님
        assertEquals(1L, outbox.countByAggregateId(loanId)) // 이벤트도 한 건만
    }
}
