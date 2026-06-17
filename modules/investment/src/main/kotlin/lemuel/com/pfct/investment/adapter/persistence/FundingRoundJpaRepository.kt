package lemuel.com.pfct.investment.adapter.persistence

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FundingRoundJpaRepository : JpaRepository<FundingRoundJpaEntity, String> {

    /**
     * 비관적 쓰기 락으로 라운드를 조회한다 → Postgres 의 `SELECT ... FOR UPDATE` 로 변환된다.
     * 같은 행에 대한 다른 트랜잭션의 락 획득은 현재 트랜잭션이 커밋/롤백될 때까지 대기한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FundingRoundJpaEntity f where f.id = :id")
    fun findByIdForUpdate(@Param("id") id: String): FundingRoundJpaEntity?
}
