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
import lemuel.com.pfct.lending.adapter.persistence.LoanJpaRepository
import lemuel.com.pfct.lending.domain.LoanStatus
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.willThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 보상 트랜잭션 검증 — 지급(3단계)이 실패하면 앞선 단계가 역순으로 되돌려져야 한다.
 * [DisburseLoanStep] 을 목으로 교체해 강제로 실패시킨다.
 */
class LoanExecutionCompensationTest @Autowired constructor(
    private val openRound: OpenFundingRoundService,
    private val invest: InvestService,
    private val saga: LoanExecutionSaga,
    private val rounds: FundingRoundJpaRepository,
    private val loans: LoanJpaRepository,
) : AbstractIntegrationTest() {

    @MockitoBean
    private lateinit var disburseLoan: DisburseLoanStep

    @Test
    fun `지급 실패 시 대출은 취소되고 라운드 실행은 되돌려진다`() {
        val roundId = "round-comp"
        val loanId = "loan-comp"
        val borrowerId = "borrower-comp"
        val principal = 1_000_000L

        // 지급 단계가 무조건 실패하도록 목을 스텁한다(인자가 결정적이므로 구체값으로 매칭).
        willThrow(RuntimeException("강제 지급 실패"))
            .given(disburseLoan).disburse(loanId, roundId, borrowerId, Money.won(principal))

        openRound.open(OpenFundingRoundCommand(roundId, Money.won(principal)))
        invest.invest(InvestCommand(FundingRoundId(roundId), InvestorId("inv-comp"), Money.won(principal)))

        assertFailsWith<LoanExecutionFailedException> {
            saga.execute(ExecuteLoanCommand(roundId, loanId, borrowerId, BigDecimal("12.0"), 12))
        }

        // 1단계 보상: 라운드는 EXECUTED → FULFILLED 로 복구
        assertEquals(FundingStatus.FULFILLED, rounds.findById(roundId).orElseThrow().status)
        // 2단계 보상: 생성됐던 대출은 CANCELLED
        assertEquals(LoanStatus.CANCELLED, loans.findById(loanId).orElseThrow().status)
    }
}
