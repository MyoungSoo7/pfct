package lemuel.com.pfct.investment.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 투자 약정 유스케이스.
 *
 * 정합성 보장의 핵심:
 * 1. `@Transactional` 로 하나의 트랜잭션 경계를 연다.
 * 2. [FundingRoundRepository.findForUpdate] 가 비관적 쓰기 락으로 행을 잠근다.
 * 3. 도메인 [lemuel.com.pfct.investment.domain.FundingRound.invest] 가 오버펀딩 불변식을 검증한다.
 * 4. 커밋 시점에 락이 해제된다.
 *
 * → 동시에 N개의 투자가 들어와도 행 단위로 직렬화되어, 목표 금액을 단 1원도 초과하지 않는다.
 */
@Service
class InvestService(
    private val repository: FundingRoundRepository,
    private val investments: InvestmentRepository,
) {
    @Transactional
    fun invest(command: InvestCommand): InvestResult {
        val round = repository.findForUpdate(command.roundId)
            ?: throw FundingRoundNotFoundException(command.roundId)

        val events = round.invest(command.investorId, command.amount)
        repository.save(round)
        // 정산을 위한 개별 투자 내역도 같은 트랜잭션으로 기록.
        investments.save(command.roundId, command.investorId, command.amount)

        return InvestResult(
            roundId = command.roundId,
            raised = round.raised,
            remaining = round.remaining,
            status = round.status,
            events = events,
        )
    }
}
