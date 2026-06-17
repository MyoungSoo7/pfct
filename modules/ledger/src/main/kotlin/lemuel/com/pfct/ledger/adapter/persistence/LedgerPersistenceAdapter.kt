package lemuel.com.pfct.ledger.adapter.persistence

import lemuel.com.pfct.common.Money
import lemuel.com.pfct.ledger.application.LedgerRepository
import lemuel.com.pfct.ledger.domain.AccountId
import lemuel.com.pfct.ledger.domain.EntryDirection
import lemuel.com.pfct.ledger.domain.LedgerTransaction
import lemuel.com.pfct.ledger.domain.TransactionId
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class LedgerPersistenceAdapter(
    private val transactionRepository: LedgerTransactionJpaRepository,
    private val journalEntryRepository: JournalEntryJpaRepository,
) : LedgerRepository {

    override fun exists(id: TransactionId): Boolean =
        transactionRepository.existsById(id.value)

    override fun append(transaction: LedgerTransaction) {
        // 헤더를 먼저 flush 하여, 분개 INSERT 시점에 FK(transaction_id) 대상 행이 존재하도록 보장한다.
        transactionRepository.saveAndFlush(
            LedgerTransactionJpaEntity(
                id = transaction.id.value,
                description = transaction.description,
                createdAt = Instant.now(),
            ),
        )
        journalEntryRepository.saveAll(
            transaction.entries.map { entry ->
                JournalEntryJpaEntity(
                    transactionId = transaction.id.value,
                    accountId = entry.accountId.value,
                    direction = entry.direction,
                    amount = entry.amount.amount.longValueExact(),
                )
            },
        )
    }

    override fun balanceOf(accountId: AccountId): Money {
        val debit = journalEntryRepository.sumAmount(accountId.value, EntryDirection.DEBIT)
        val credit = journalEntryRepository.sumAmount(accountId.value, EntryDirection.CREDIT)
        return Money.won(debit - credit)
    }
}
