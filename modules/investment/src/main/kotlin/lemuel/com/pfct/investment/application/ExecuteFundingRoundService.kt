package lemuel.com.pfct.investment.application

import lemuel.com.pfct.common.Money
import lemuel.com.pfct.investment.domain.FundingRoundId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 대출 실행 Saga의 투자 측 단계.
 * - [execute]: 라운드를 EXECUTED로 확정하고 실행 원금을 반환(멱등).
 * - [compensate]: 후속 단계 실패 시 실행을 되돌림(EXECUTED → FULFILLED).
 *
 * 각 메서드는 독립 트랜잭션이다. Saga 오케스트레이터가 단계별로 호출하고, 실패 시 보상을 부른다.
 */
@Service
class ExecuteFundingRoundService(
    private val repository: FundingRoundRepository,
) {
    @Transactional
    fun execute(roundId: FundingRoundId): Money {
        val round = repository.findForUpdate(roundId)
            ?: throw FundingRoundNotFoundException(roundId)
        val principal = round.execute()
        repository.save(round)
        return principal
    }

    @Transactional
    fun compensate(roundId: FundingRoundId) {
        val round = repository.findForUpdate(roundId)
            ?: throw FundingRoundNotFoundException(roundId)
        round.revertExecution()
        repository.save(round)
    }
}
