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
 *
 * 발행이 실패하면 [attempts] 가 늘고 [nextAttemptAt] 으로 백오프 재시도를 예약한다.
 * 재시도 한도를 넘기면 [dead] 로 격리(DLQ)되어 더는 폴링되지 않는다.
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

    /** 누적 발행 실패 횟수. */
    @Column(name = "attempts", nullable = false)
    var attempts: Int = 0,

    /** 다음 재시도 가능 시각. null 이면 즉시 발행 후보. */
    @Column(name = "next_attempt_at")
    var nextAttemptAt: Instant? = null,

    /** 마지막 실패 사유(운영 진단용). */
    @Column(name = "last_error", columnDefinition = "text")
    var lastError: String? = null,

    /** 재시도 한도를 넘겨 DLQ 로 격리된 이벤트. */
    @Column(name = "dead", nullable = false)
    var dead: Boolean = false,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
)
