package lemuel.com.pfct.settlement

import lemuel.com.pfct.AbstractIntegrationTest
import lemuel.com.pfct.common.AnnualInterestRate
import lemuel.com.pfct.common.Money
import lemuel.com.pfct.investment.application.InvestCommand
import lemuel.com.pfct.investment.application.InvestService
import lemuel.com.pfct.investment.application.OpenFundingRoundCommand
import lemuel.com.pfct.investment.application.OpenFundingRoundService
import lemuel.com.pfct.investment.domain.FundingRoundId
import lemuel.com.pfct.investment.domain.InvestorId
import lemuel.com.pfct.ledger.application.LedgerRepository
import lemuel.com.pfct.ledger.domain.AccountId
import lemuel.com.pfct.lending.domain.EqualPaymentScheduleGenerator
import lemuel.com.pfct.saga.ExecuteLoanCommand
import lemuel.com.pfct.saga.LoanExecutionSaga
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettlementIntegrationTest @Autowired constructor(
    private val openRound: OpenFundingRoundService,
    private val invest: InvestService,
    private val saga: LoanExecutionSaga,
    private val settle: SettleRepaymentService,
    private val ledger: LedgerRepository,
) : AbstractIntegrationTest() {

    @Test
    fun `상환금이 투자 비율대로 분배되고 수수료를 떼며 원장이 균형을 이룬다`() {
        val roundId = "settle-round"
        val loanId = "settle-loan"
        val borrowerId = "settle-borrower"
        val principal = 1_000_000L

        // 투자자 A:B = 6:4 로 모집 완료
        openRound.open(OpenFundingRoundCommand(roundId, Money.won(principal)))
        invest.invest(InvestCommand(FundingRoundId(roundId), InvestorId("A"), Money.won(600_000)))
        invest.invest(InvestCommand(FundingRoundId(roundId), InvestorId("B"), Money.won(400_000)))

        // 대출 실행(연 12%, 12개월)
        saga.execute(ExecuteLoanCommand(roundId, loanId, borrowerId, BigDecimal("12.0"), 12))

        // 1회차 정산 (이자 수수료 10%)
        val first = settle.settle(SettleRepaymentCommand(loanId, roundId, sequence = 1, feeRatePercent = BigDecimal("10")))
        assertTrue(first.applied)

        // 기대값: 동일한 스케줄/수수료 규칙으로 직접 계산
        val installment = EqualPaymentScheduleGenerator
            .generate(Money.won(principal), AnnualInterestRate.of("12.0"), 12)
            .first { it.sequence == 1 }
        val expectedFee = Money.won(installment.interest.amount.toLong() * 10 / 100)
        val distributable = installment.principal + (installment.interest - expectedFee)

        val balA = ledger.balanceOf(AccountId("investor:A"))
        val balB = ledger.balanceOf(AccountId("investor:B"))
        val balFee = ledger.balanceOf(AccountId("platform:fee"))

        assertEquals(expectedFee, balFee, "수수료 계정 잔액")
        assertEquals(distributable, balA + balB, "투자자 분배 합 = 원금 + (이자 − 수수료)")
        // 돈의 보존: 투자자 분배 + 수수료 = 차주가 낸 원리금
        assertEquals(installment.total, balA + balB + balFee)
        // 비율 6:4 → A가 B보다 많이 받음
        assertTrue(balA > balB, "6:4 비율이면 A가 더 많아야 한다")
    }

    @Test
    fun `같은 회차를 두 번 정산해도 한 번만 반영된다 — 멱등`() {
        val roundId = "settle-idem-round"
        val loanId = "settle-idem-loan"
        val borrowerId = "settle-idem-borrower"
        openRound.open(OpenFundingRoundCommand(roundId, Money.won(1_000_000)))
        invest.invest(InvestCommand(FundingRoundId(roundId), InvestorId("X"), Money.won(1_000_000)))
        saga.execute(ExecuteLoanCommand(roundId, loanId, borrowerId, BigDecimal("12.0"), 12))

        val command = SettleRepaymentCommand(loanId, roundId, sequence = 1, feeRatePercent = BigDecimal("10"))
        val first = settle.settle(command)
        val balanceAfterFirst = ledger.balanceOf(AccountId("investor:X"))
        val second = settle.settle(command)

        assertTrue(first.applied)
        assertFalse(second.applied, "두 번째 정산은 멱등 처리되어야 한다")
        assertEquals(balanceAfterFirst, ledger.balanceOf(AccountId("investor:X")), "잔액 중복 반영 없음")
    }
}
