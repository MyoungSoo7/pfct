package lemuel.com.pfct.ledger.domain

import lemuel.com.pfct.common.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LedgerTransactionTest {

    private val investor = AccountId("investor:1")
    private val borrower = AccountId("borrower:1")
    private val txId = TransactionId("tx-1")

    @Test
    fun `차변과 대변이 일치하면 거래가 생성된다`() {
        val tx = LedgerTransaction.of(
            id = txId,
            description = "대출 실행: 투자자 → 차주",
            entries = listOf(
                JournalEntry(investor, EntryDirection.CREDIT, Money.won(1_000_000)),
                JournalEntry(borrower, EntryDirection.DEBIT, Money.won(1_000_000)),
            ),
        )
        assertEquals(Money.won(1_000_000), tx.totalDebit)
        assertEquals(tx.totalDebit, tx.totalCredit)
    }

    @Test
    fun `차변과 대변이 어긋나면 거래 생성이 거부된다 — 돈이 사라질 수 없다`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            LedgerTransaction.of(
                id = txId,
                description = "불균형 거래",
                entries = listOf(
                    JournalEntry(investor, EntryDirection.CREDIT, Money.won(1_000_000)),
                    JournalEntry(borrower, EntryDirection.DEBIT, Money.won(999_999)),
                ),
            )
        }
        assertEquals(true, ex.message!!.contains("복식부기 불변식 위반"))
    }

    @Test
    fun `분개가 하나뿐이면 거부된다`() {
        assertFailsWith<IllegalArgumentException> {
            LedgerTransaction.of(
                id = txId,
                description = "단일 분개",
                entries = listOf(JournalEntry(investor, EntryDirection.DEBIT, Money.won(100))),
            )
        }
    }

    @Test
    fun `여러 투자자가 한 차주에게 — 다대일 분개도 균형이 맞으면 생성된다`() {
        val tx = LedgerTransaction.of(
            id = txId,
            description = "2명의 투자자 합산 실행",
            entries = listOf(
                JournalEntry(AccountId("investor:1"), EntryDirection.CREDIT, Money.won(600_000)),
                JournalEntry(AccountId("investor:2"), EntryDirection.CREDIT, Money.won(400_000)),
                JournalEntry(borrower, EntryDirection.DEBIT, Money.won(1_000_000)),
            ),
        )
        assertEquals(tx.totalDebit, tx.totalCredit)
    }
}
