package lemuel.com.pfct.outbox

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 아웃박스 릴레이. 미발행 이벤트를 주기적으로 polling 하여 발행하고 `published_at` 을 찍는다.
 * 발행이 실패하면 published_at 이 그대로 null 이므로 다음 폴링에서 재시도된다(at-least-once).
 *
 * 테스트에서는 스케줄을 기다리지 않고 [publishPending] 를 직접 호출해 결정적으로 검증한다.
 */
@Component
class OutboxRelay(
    private val repository: OutboxJpaRepository,
    private val publisher: EventPublisher,
) {
    @Scheduled(fixedDelayString = "\${pfct.outbox.relay-delay-ms:2000}")
    @Transactional
    fun publishPending(): Int {
        val pending = repository.findTop100ByPublishedAtIsNullOrderByIdAsc()
        pending.forEach { event ->
            publisher.publish(event)
            event.publishedAt = Instant.now()
        }
        return pending.size
    }
}
