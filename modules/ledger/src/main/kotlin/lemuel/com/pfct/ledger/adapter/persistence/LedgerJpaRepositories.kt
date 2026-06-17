package lemuel.com.pfct.ledger.adapter.persistence

import lemuel.com.pfct.ledger.domain.EntryDirection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LedgerTransactionJpaRepository : JpaRepository<LedgerTransactionJpaEntity, String>

interface JournalEntryJpaRepository : JpaRepository<JournalEntryJpaEntity, Long> {

    /** 특정 계정의 특정 방향(차변/대변) 합계. 없으면 0. */
    @Query(
        "select coalesce(sum(e.amount), 0) from JournalEntryJpaEntity e " +
            "where e.accountId = :accountId and e.direction = :direction",
    )
    fun sumAmount(
        @Param("accountId") accountId: String,
        @Param("direction") direction: EntryDirection,
    ): Long
}
