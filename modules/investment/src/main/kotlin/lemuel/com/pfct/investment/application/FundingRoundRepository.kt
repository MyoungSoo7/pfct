package lemuel.com.pfct.investment.application

import lemuel.com.pfct.investment.domain.FundingRound
import lemuel.com.pfct.investment.domain.FundingRoundId

/**
 * 펀딩 라운드 영속화 포트(application 계층이 소유하는 인터페이스).
 *
 * 구현체(JPA 어댑터)는 adapter 계층에 있고, 의존성 방향은 항상 adapter → application 이다.
 * application/domain 은 JPA 를 전혀 모른다.
 */
interface FundingRoundRepository {

    /**
     * 비관적 쓰기 락(SELECT ... FOR UPDATE)으로 라운드를 조회한다.
     * 동시에 여러 투자가 들어와도 이 락이 트랜잭션 동안 행을 직렬화하여 오버펀딩을 막는다.
     */
    fun findForUpdate(id: FundingRoundId): FundingRound?

    /** 락 없이 단순 조회(읽기 전용). */
    fun findById(id: FundingRoundId): FundingRound?

    fun save(round: FundingRound): FundingRound
}
