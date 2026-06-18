package lemuel.com.pfct.investment.adapter.persistence

import lemuel.com.pfct.common.Money
import lemuel.com.pfct.investment.application.InvestmentRepository
import lemuel.com.pfct.investment.application.InvestorShare
import lemuel.com.pfct.investment.domain.FundingRoundId
import lemuel.com.pfct.investment.domain.InvestorId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

interface InvestmentJpaRepository : JpaRepository<InvestmentJpaEntity, Long> {
    fun findByRoundId(roundId: String): List<InvestmentJpaEntity>
}

@Component
class InvestmentPersistenceAdapter(
    private val jpaRepository: InvestmentJpaRepository,
) : InvestmentRepository {

    override fun save(roundId: FundingRoundId, investorId: InvestorId, amount: Money) {
        jpaRepository.save(
            InvestmentJpaEntity(
                roundId = roundId.value,
                investorId = investorId.value,
                amount = amount.amount.longValueExact(),
            ),
        )
    }

    override fun findSharesByRound(roundId: FundingRoundId): List<InvestorShare> =
        jpaRepository.findByRoundId(roundId.value)
            .groupBy { it.investorId }
            .map { (investorId, rows) ->
                InvestorShare(InvestorId(investorId), Money.won(rows.sumOf { it.amount }))
            }
}
