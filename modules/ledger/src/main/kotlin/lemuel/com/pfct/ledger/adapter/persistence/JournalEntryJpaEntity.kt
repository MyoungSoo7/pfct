package lemuel.com.pfct.ledger.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import lemuel.com.pfct.ledger.domain.EntryDirection

/**
 * 분개 한 줄(append-only). `transaction_id` 는 헤더로의 논리 FK(DB 제약으로 무결성 보장).
 * JPA 연관관계 대신 단순 컬럼으로 두어 매핑을 단순화했다.
 */
@Entity
@Table(name = "journal_entry")
class JournalEntryJpaEntity(
    @Column(name = "transaction_id", nullable = false, length = 64)
    val transactionId: String,

    @Column(name = "account_id", nullable = false, length = 64)
    val accountId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 8)
    val direction: EntryDirection,

    @Column(name = "amount", nullable = false)
    val amount: Long,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
)
