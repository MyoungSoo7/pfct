package lemuel.com.pfct.ledger

import lemuel.com.pfct.AbstractIntegrationTest
import lemuel.com.pfct.common.Money
import lemuel.com.pfct.ledger.application.LedgerRepository
import lemuel.com.pfct.ledger.application.RecordLedgerTransactionService
import lemuel.com.pfct.ledger.domain.AccountId
import lemuel.com.pfct.ledger.domain.EntryDirection
import lemuel.com.pfct.ledger.domain.JournalEntry
import lemuel.com.pfct.ledger.domain.LedgerTransaction
import lemuel.com.pfct.ledger.domain.TransactionId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LedgerPersistenceIntegrationTest @Autowired constructor(
    private val recordService: RecordLedgerTransactionService,
    private val ledger: LedgerRepository,
) : AbstractIntegrationTest() {

    @Test
    fun `균형 잡힌 거래를 기록하면 계정 잔액이 차변+ 대변- 로 도출된다`() {
        val investor = AccountId("investor:balance-test")
        val borrower = AccountId("borrower:balance-test")
        val tx = LedgerTransaction.of(
            id = TransactionId("disburse:balance-test"),
            description = "대출 실행: 투자자 → 차주",
            entries = listOf(
                JournalEntry(investor, EntryDirection.CREDIT, Money.won(1_000_000)),
                JournalEntry(borrower, EntryDirection.DEBIT, Money.won(1_000_000)),
            ),
        )

        val result = recordService.record(tx)

        assertTrue(result.applied)
        assertEquals(Money.won(1_000_000), ledger.balanceOf(borrower))   // 차변 → +
        assertEquals(Money.won(-1_000_000), ledger.balanceOf(investor))  // 대변 → −
        // 돈의 보존: 두 잔액의 합은 0
        assertEquals(Money.ZERO, ledger.balanceOf(borrower) + ledger.balanceOf(investor))
    }

    @Test
    fun `같은 거래 ID로 두 번 기록해도 한 번만 반영된다 — 멱등성`() {
        val investor = AccountId("investor:idem-test")
        val borrower = AccountId("borrower:idem-test")
        fun newTx() = LedgerTransaction.of(
            id = TransactionId("disburse:idem-test"),
            description = "중복 요청",
            entries = listOf(
                JournalEntry(investor, EntryDirection.CREDIT, Money.won(500_000)),
                JournalEntry(borrower, EntryDirection.DEBIT, Money.won(500_000)),
            ),
        )

        val first = recordService.record(newTx())
        val second = recordService.record(newTx())

        assertTrue(first.applied, "첫 기록은 반영되어야 한다")
        assertFalse(second.applied, "두 번째는 멱등 처리되어 반영되지 않아야 한다")
        // 잔액은 한 번만 반영(중복 가산 없음)
        assertEquals(Money.won(500_000), ledger.balanceOf(borrower))
    }
}
