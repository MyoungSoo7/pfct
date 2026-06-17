package lemuel.com.pfct.investment.adapter.persistence

import lemuel.com.pfct.common.Money
import lemuel.com.pfct.investment.application.FundingRoundRepository
import lemuel.com.pfct.investment.domain.FundingRound
import lemuel.com.pfct.investment.domain.FundingRoundId
import org.springframework.stereotype.Component

/**
 * [FundingRoundRepository] 포트의 JPA 구현. 도메인 ↔ 엔티티 매핑을 전담한다.
 */
@Component
class FundingRoundPersistenceAdapter(
    private val jpaRepository: FundingRoundJpaRepository,
) : FundingRoundRepository {

    override fun findForUpdate(id: FundingRoundId): FundingRound? =
        jpaRepository.findByIdForUpdate(id.value)?.toDomain()

    override fun findById(id: FundingRoundId): FundingRound? =
        jpaRepository.findById(id.value).orElse(null)?.toDomain()

    override fun save(round: FundingRound): FundingRound {
        jpaRepository.save(round.toEntity())
        return round
    }
}

private fun FundingRoundJpaEntity.toDomain(): FundingRound =
    FundingRound(
        id = FundingRoundId(id),
        targetAmount = Money.won(targetAmount),
        raised = Money.won(raisedAmount),
        status = status,
    )

private fun FundingRound.toEntity(): FundingRoundJpaEntity =
    FundingRoundJpaEntity(
        id = id.value,
        targetAmount = targetAmount.amount.longValueExact(),
        raisedAmount = raised.amount.longValueExact(),
        status = status,
    )
