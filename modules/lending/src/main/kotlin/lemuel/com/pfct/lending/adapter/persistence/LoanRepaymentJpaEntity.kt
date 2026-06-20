package lemuel.com.pfct.lending.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import lemuel.com.pfct.lending.domain.RepaymentStatus
import java.time.LocalDate

@Entity
@Table(name = "loan_repayment")
class LoanRepaymentJpaEntity(
    @Column(name = "loan_id", nullable = false, length = 64)
    val loanId: String,

    @Column(name = "sequence", nullable = false)
    val sequence: Int,

    @Column(name = "due_date", nullable = false)
    val dueDate: LocalDate,

    @Column(name = "principal", nullable = false)
    val principal: Long,

    @Column(name = "interest", nullable = false)
    val interest: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: RepaymentStatus,

    @Column(name = "late_fee", nullable = false)
    var lateFee: Long,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
)
