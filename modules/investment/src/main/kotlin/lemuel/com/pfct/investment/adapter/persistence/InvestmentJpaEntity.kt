package lemuel.com.pfct.investment.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/** 개별 투자 내역(append-only). 한 투자자가 한 라운드에 여러 번 투자할 수 있으므로 각 투자가 한 행이다. */
@Entity
@Table(name = "investment")
class InvestmentJpaEntity(
    @Column(name = "round_id", nullable = false, length = 64)
    val roundId: String,

    @Column(name = "investor_id", nullable = false, length = 64)
    val investorId: String,

    @Column(name = "amount", nullable = false)
    val amount: Long,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
)
