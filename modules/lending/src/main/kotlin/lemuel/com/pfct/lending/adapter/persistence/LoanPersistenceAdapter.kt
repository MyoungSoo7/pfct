package lemuel.com.pfct.lending.adapter.persistence

import lemuel.com.pfct.common.AnnualInterestRate
import lemuel.com.pfct.common.Money
import lemuel.com.pfct.lending.application.LoanRepository
import lemuel.com.pfct.lending.domain.BorrowerId
import lemuel.com.pfct.lending.domain.Loan
import lemuel.com.pfct.lending.domain.LoanId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

interface LoanJpaRepository : JpaRepository<LoanJpaEntity, String>

@Component
class LoanPersistenceAdapter(
    private val jpaRepository: LoanJpaRepository,
) : LoanRepository {

    override fun existsById(id: LoanId): Boolean =
        jpaRepository.existsById(id.value)

    override fun findById(id: LoanId): Loan? =
        jpaRepository.findById(id.value).orElse(null)?.toDomain()

    override fun save(loan: Loan): Loan {
        jpaRepository.save(loan.toEntity())
        return loan
    }
}

private fun LoanJpaEntity.toDomain(): Loan =
    Loan(
        id = LoanId(id),
        borrowerId = BorrowerId(borrowerId),
        principal = Money.won(principal),
        annualRate = AnnualInterestRate(annualRatePercent),
        months = months,
        status = status,
    )

private fun Loan.toEntity(): LoanJpaEntity =
    LoanJpaEntity(
        id = id.value,
        borrowerId = borrowerId.value,
        principal = principal.amount.longValueExact(),
        annualRatePercent = annualRate.percent,
        months = months,
        status = status,
    )
