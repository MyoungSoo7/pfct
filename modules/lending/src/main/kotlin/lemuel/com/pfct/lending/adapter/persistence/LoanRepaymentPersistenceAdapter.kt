package lemuel.com.pfct.lending.adapter.persistence

import lemuel.com.pfct.common.Money
import lemuel.com.pfct.lending.application.LoanRepaymentRepository
import lemuel.com.pfct.lending.domain.LoanId
import lemuel.com.pfct.lending.domain.RepaymentStatus
import lemuel.com.pfct.lending.domain.ScheduledRepayment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

interface LoanRepaymentJpaRepository : JpaRepository<LoanRepaymentJpaEntity, Long> {
    fun findByLoanIdAndSequence(loanId: String, sequence: Int): LoanRepaymentJpaEntity?
    fun findByLoanIdOrderBySequenceAsc(loanId: String): List<LoanRepaymentJpaEntity>
    fun findByStatusAndDueDateBefore(status: RepaymentStatus, dueDate: LocalDate): List<LoanRepaymentJpaEntity>
}

@Component
class LoanRepaymentPersistenceAdapter(
    private val jpaRepository: LoanRepaymentJpaRepository,
) : LoanRepaymentRepository {

    override fun saveAll(entries: List<ScheduledRepayment>) {
        jpaRepository.saveAll(entries.map { it.toEntity() })
    }

    override fun update(entry: ScheduledRepayment) {
        val managed = jpaRepository.findByLoanIdAndSequence(entry.loanId.value, entry.sequence)
            ?: throw IllegalStateException("상환 회차를 찾을 수 없습니다: ${entry.loanId.value} #${entry.sequence}")
        managed.status = entry.status
        managed.lateFee = entry.lateFee.amount.longValueExact()
        jpaRepository.save(managed)
    }

    override fun findByLoanIdAndSequence(loanId: LoanId, sequence: Int): ScheduledRepayment? =
        jpaRepository.findByLoanIdAndSequence(loanId.value, sequence)?.toDomain()

    override fun findByLoanId(loanId: LoanId): List<ScheduledRepayment> =
        jpaRepository.findByLoanIdOrderBySequenceAsc(loanId.value).map { it.toDomain() }

    override fun findOverdueCandidates(asOf: LocalDate): List<ScheduledRepayment> =
        jpaRepository.findByStatusAndDueDateBefore(RepaymentStatus.DUE, asOf).map { it.toDomain() }
}

private fun LoanRepaymentJpaEntity.toDomain(): ScheduledRepayment =
    ScheduledRepayment(
        loanId = LoanId(loanId),
        sequence = sequence,
        dueDate = dueDate,
        principal = Money.won(principal),
        interest = Money.won(interest),
        status = status,
        lateFee = Money.won(lateFee),
    )

private fun ScheduledRepayment.toEntity(): LoanRepaymentJpaEntity =
    LoanRepaymentJpaEntity(
        loanId = loanId.value,
        sequence = sequence,
        dueDate = dueDate,
        principal = principal.amount.longValueExact(),
        interest = interest.amount.longValueExact(),
        status = status,
        lateFee = lateFee.amount.longValueExact(),
    )
