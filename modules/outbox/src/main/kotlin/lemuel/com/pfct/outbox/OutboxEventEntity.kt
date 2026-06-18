package lemuel.com.pfct.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 아웃박스 이벤트. 발행 대상 메시지를 DB에 먼저 적재한다(상태 변경과 같은 트랜잭션).
 * `published_at` 이 null 이면 아직 발행되지 않은 이벤트다.
 */
@Entity
@Table(name = "outbox_event")
class OutboxEventEntity(
    @Column(name = "aggregate_type", nullable = false, length = 64)
    val aggregateType: String,

    @Column(name = "aggregate_id", nullable = false, length = 64)
    val aggregateId: String,

    @Column(name = "event_type", nullable = false, length = 128)
    val eventType: String,

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    val payload: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "published_at")
    var publishedAt: Instant? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
)
