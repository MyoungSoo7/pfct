package lemuel.com.pfct.outbox

import org.springframework.data.jpa.repository.JpaRepository

interface OutboxJpaRepository : JpaRepository<OutboxEventEntity, Long> {

    /** 아직 발행되지 않은 이벤트를 ID 순으로(=발생 순서) 최대 100건 가져온다. */
    fun findTop100ByPublishedAtIsNullOrderByIdAsc(): List<OutboxEventEntity>

    fun countByEventType(eventType: String): Long

    fun countByAggregateId(aggregateId: String): Long
}
