package lemuel.com.pfct.outbox

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import java.time.Instant

interface OutboxJpaRepository : JpaRepository<OutboxEventEntity, Long> {

    /** 아직 발행되지 않은 이벤트를 ID 순으로(=발생 순서) 최대 100건 가져온다. */
    fun findTop100ByPublishedAtIsNullOrderByIdAsc(): List<OutboxEventEntity>

    /**
     * 발행 후보: 아직 미발행이고, DLQ 격리되지 않았으며, 재시도 대기(`nextAttemptAt`)가 끝난 이벤트.
     * 발생 순서(id asc)를 보존한다.
     *
     * **`FOR UPDATE SKIP LOCKED`** — 호출자의 트랜잭션이 잡은 행에 행 잠금을 걸고, *다른* 트랜잭션이
     * 이미 잠근 행은 건너뛴다. 덕분에 릴레이를 여러 인스턴스로 띄워도 각 인스턴스가 **서로 겹치지 않는**
     * 후보 집합을 가져가, 중복 발행/경합 없이 수평 확장된다. 반드시 호출자 트랜잭션 안에서 select→발행→
     * `published_at` 갱신→commit 이 한 단위로 일어나야 잠금이 그 사이 유지된다.
     *
     * (JPA `PESSIMISTIC_WRITE` + 잠금 타임아웃 힌트 `-2` = Hibernate `LockOptions.SKIP_LOCKED`.)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query(
        """
        select e from OutboxEventEntity e
        where e.publishedAt is null
          and e.dead = false
          and (e.nextAttemptAt is null or e.nextAttemptAt <= :now)
        order by e.id asc
        """,
    )
    fun findReadyForPublish(@Param("now") now: Instant, pageable: Pageable): List<OutboxEventEntity>

    /** DLQ 운영 조회: 격리된 이벤트를 ID 순으로. */
    fun findByDeadTrueOrderByIdAsc(): List<OutboxEventEntity>

    /** 아직 발행되지 않은(격리 제외) 이벤트 수 — 잠금 없이. 멀티스레드 진행 종료 판정 등에 쓴다. */
    fun countByPublishedAtIsNullAndDeadFalse(): Long

    fun countByEventType(eventType: String): Long

    fun countByAggregateId(aggregateId: String): Long
}
