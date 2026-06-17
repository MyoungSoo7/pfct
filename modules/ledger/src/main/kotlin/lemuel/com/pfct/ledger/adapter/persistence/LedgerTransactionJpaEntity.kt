package lemuel.com.pfct.ledger.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/** 원장 거래 헤더. PK(id)가 멱등 키 역할을 한다(같은 ID 재기록 시 유니크 제약 위반). */
@Entity
@Table(name = "ledger_transaction")
class LedgerTransactionJpaEntity(
    @Id
    @Column(name = "id", nullable = false, length = 64)
    val id: String,

    @Column(name = "description", nullable = false)
    val description: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
