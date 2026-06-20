package lemuel.com.pfct.overdue

import lemuel.com.pfct.AbstractIntegrationTest
import lemuel.com.pfct.common.Money
import lemuel.com.pfct.investment.application.InvestCommand
import lemuel.com.pfct.investment.application.InvestService
import lemuel.com.pfct.investment.application.OpenFundingRoundCommand
import lemuel.com.pfct.investment.application.OpenFundingRoundService
import lemuel.com.pfct.investment.domain.FundingRoundId
import lemuel.com.pfct.investment.domain.InvestorId
import lemuel.com.pfct.lending.application.LoanRepaymentRepository
import lemuel.com.pfct.lending.domain.LoanId
import lemuel.com.pfct.lending.domain.RepaymentStatus
import lemuel.com.pfct.saga.ExecuteLoanCommand
import lemuel.com.pfct.saga.LoanExecutionSaga
import lemuel.com.pfct.settlement.SettleRepaymentCommand
import lemuel.com.pfct.settlement.SettleRepaymentService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverdueIntegrationTest @Autowired constructor(
    private val openRound: OpenFundingRoundService,
    private val invest: InvestService,
    private val saga: LoanExecutionSaga,
    private val settle: SettleRepaymentService,
    private val repayments: LoanRepaymentRepository,
    private val scanner: OverdueScanner,
) : AbstractIntegrationTest() {

    @Test
    fun `납기 지난 미상환 회차는 연체 처리되고, 납입한 회차는 제외된다`() {
        val roundId = "od-round"
        val loanId = "od-loan"
        val principal = 1_200_000L
        // 6개월 전에 실행된 대출 → 1~5회차 납기는 이미 과거(연체 대상), 6회차 이후는 미래.
        val startDate = LocalDate.now().minusMonths(6)

        openRound.open(OpenFundingRoundCommand(roundId, Money.won(principal)))
        invest.invest(InvestCommand(FundingRoundId(roundId), InvestorId("od-investor"), Money.won(principal)))
        saga.execute(ExecuteLoanCommand(roundId, loanId, "od-borrower", BigDecimal("12.0"), 12, startDate))

        // 1회차는 정산(상환) 완료 → 연체 대상에서 빠져야 한다.
        settle.settle(SettleRepaymentCommand(loanId, roundId, sequence = 1, feeRatePercent = BigDecimal("10")))

        // 연체 스캔(동기 실행). 이 호출 이후 미상환·납기경과 회차는 OVERDUE 로 확정된다.
        scanner.scan()

        val entries = repayments.findByLoanId(LoanId(loanId)).associateBy { it.sequence }
        assertEquals(RepaymentStatus.PAID, entries.getValue(1).status, "1회차는 납입 완료")
        for (seq in 2..5) {
            assertEquals(RepaymentStatus.OVERDUE, entries.getValue(seq).status, "${seq}회차는 연체")
            assertTrue(entries.getValue(seq).lateFee.isPositive(), "${seq}회차 연체료 > 0")
        }
        assertEquals(RepaymentStatus.DUE, entries.getValue(12).status, "마지막 회차는 아직 미래라 DUE")
    }
}
