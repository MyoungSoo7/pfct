package lemuel.com.pfct.lending.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import lemuel.com.pfct.lending.domain.LoanStatus
import java.math.BigDecimal

@Entity
@Table(name = "loan")
class LoanJpaEntity(
    @Id
    @Column(name = "id", nullable = false, length = 64)
    val id: String,

    @Column(name = "borrower_id", nullable = false, length = 64)
    val borrowerId: String,

    @Column(name = "principal", nullable = false)
    val principal: Long,

    @Column(name = "annual_rate_percent", nullable = false, precision = 5, scale = 2)
    val annualRatePercent: BigDecimal,

    @Column(name = "months", nullable = false)
    val months: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: LoanStatus,
)
