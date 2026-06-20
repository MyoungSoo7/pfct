package lemuel.com.pfct.settlement

import lemuel.com.pfct.common.Money
import lemuel.com.pfct.investment.application.InvestmentRepository
import lemuel.com.pfct.investment.domain.FundingRoundId
import lemuel.com.pfct.ledger.application.RecordLedgerTransactionService
import lemuel.com.pfct.ledger.domain.AccountId
import lemuel.com.pfct.ledger.domain.EntryDirection
import lemuel.com.pfct.ledger.domain.JournalEntry
import lemuel.com.pfct.ledger.domain.LedgerTransaction
import lemuel.com.pfct.ledger.domain.TransactionId
import com.fasterxml.jackson.databind.ObjectMapper
import lemuel.com.pfct.event.InvestorDistribution
import lemuel.com.pfct.event.RepaymentSettledEvent
import lemuel.com.pfct.lending.application.LoanRepaymentRepository
import lemuel.com.pfct.lending.application.LoanRepository
import lemuel.com.pfct.lending.domain.LoanId
import lemuel.com.pfct.outbox.OutboxRecorder
import lemuel.com.pfct.settlement.domain.ProRataDistributor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 상환 정산 — 차주가 납입한 한 회차(원리금)를 투자자에게 비율대로 분배하고 플랫폼 수수료를 뗀다.
 *
 * 원장 분개(차변=대변 보장):
 *  - 대변(CREDIT) borrower      : 원금 + 이자 (차주가 납입)
 *  - 차변(DEBIT)  investor[i]   : 비율 분배액 (= 원금 + (이자 − 수수료)를 투자 비율로)
 *  - 차변(DEBIT)  platform:fee  : 이자에 대한 수수료
 *
 * 멱등: 같은 (대출, 회차)로 두 번 정산해도 원장이 한 번만 반영된다(거래 ID = `settle:{loanId}:{seq}`).
 */
@Service
class SettleRepaymentService(
    private val loans: LoanRepository,
    private val investments: InvestmentRepository,
    private val ledger: RecordLedgerTransactionService,
    private val repayments: LoanRepaymentRepository,
    private val outbox: OutboxRecorder,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun settle(command: SettleRepaymentCommand): SettlementResult {
        val loan = loans.findById(LoanId(command.loanId))
            ?: throw IllegalArgumentException("대출을 찾을 수 없습니다: ${command.loanId}")
        val installment = loan.repaymentSchedule().firstOrNull { it.sequence == command.sequence }
            ?: throw IllegalArgumentException("존재하지 않는 상환 회차입니다: ${command.sequence}")

        val shares = investments.findSharesByRound(FundingRoundId(command.roundId))
        require(shares.isNotEmpty()) { "라운드에 투자 내역이 없습니다: ${command.roundId}" }

        val fee = feeOn(installment.interest, command.feeRatePercent)
        val distributable = installment.principal + (installment.interest - fee)
        val amounts = ProRataDistributor.distribute(distributable, shares.map { it.amount })

        val entries = buildList {
            add(JournalEntry(AccountId("borrower:${loan.borrowerId.value}"), EntryDirection.CREDIT, installment.total))
            shares.forEachIndexed { i, share ->
                if (amounts[i].isPositive()) {
                    add(JournalEntry(AccountId("investor:${share.investorId.value}"), EntryDirection.DEBIT, amounts[i]))
                }
            }
            if (fee.isPositive()) {
                add(JournalEntry(AccountId("platform:fee"), EntryDirection.DEBIT, fee))
            }
        }

        val settlementId = "settle:${command.loanId}:${command.sequence}"
        val distributions = shares.mapIndexed { i, s -> s.investorId.value to amounts[i] }
        val result = ledger.record(
            LedgerTransaction.of(
                id = TransactionId(settlementId),
                description = "상환 정산 loan=${command.loanId} 회차=${command.sequence}",
                entries = entries,
            ),
        )

        // 실제 반영된 경우에만 해당 회차를 PAID 로 전이하고(연체 대상에서 제외) 정산 이벤트를 아웃박스에 적재.
        if (result.applied) {
            repayments.findByLoanIdAndSequence(LoanId(command.loanId), command.sequence)?.let { entry ->
                entry.markPaid()
                repayments.update(entry)
            }
            val event = RepaymentSettledEvent(
                settlementId = settlementId,
                loanId = command.loanId,
                distributions = distributions
                    .filter { (_, amount) -> amount.isPositive() }
                    .map { (investorId, amount) -> InvestorDistribution(investorId, amount.amount.longValueExact()) },
            )
            outbox.record("Settlement", settlementId, "RepaymentSettled", objectMapper.writeValueAsString(event))
        }

        return SettlementResult(
            loanId = command.loanId,
            sequence = command.sequence,
            fee = fee,
            distributions = distributions,
            applied = result.applied,
        )
    }

    /** 이자에 대한 수수료 = floor(interest × feeRatePercent / 100). 원 단위 절사. */
    private fun feeOn(interest: Money, feeRatePercent: BigDecimal): Money {
        val feeWon = interest.amount
            .multiply(feeRatePercent)
            .divide(BigDecimal(100), 0, RoundingMode.DOWN)
        return Money.won(feeWon)
    }
}

data class SettleRepaymentCommand(
    val loanId: String,
    val roundId: String,
    val sequence: Int,
    val feeRatePercent: BigDecimal,
)

data class SettlementResult(
    val loanId: String,
    val sequence: Int,
    val fee: Money,
    val distributions: List<Pair<String, Money>>,
    val applied: Boolean,
)
