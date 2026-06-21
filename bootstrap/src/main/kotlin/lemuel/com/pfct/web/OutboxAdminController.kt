package lemuel.com.pfct.web

import lemuel.com.pfct.outbox.OutboxJpaRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 운영용: DLQ(데드레터)로 격리된 아웃박스 이벤트를 조회한다. */
@RestController
@RequestMapping("/api/admin/outbox")
class OutboxAdminController(
    private val outbox: OutboxJpaRepository,
) {
    @GetMapping("/dead")
    fun deadLetters(): List<DeadLetterView> =
        outbox.findByDeadTrueOrderByIdAsc().map { e ->
            DeadLetterView(
                id = e.id,
                aggregateType = e.aggregateType,
                aggregateId = e.aggregateId,
                eventType = e.eventType,
                attempts = e.attempts,
                lastError = e.lastError,
                createdAt = e.createdAt.toString(),
            )
        }
}

data class DeadLetterView(
    val id: Long?,
    val aggregateType: String,
    val aggregateId: String,
    val eventType: String,
    val attempts: Int,
    val lastError: String?,
    val createdAt: String,
)
