package lemuel.com.pfct.outbox

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 아웃박스 이벤트의 실제 발행 대상. Phase C에서 Kafka 구현으로 교체될 자리다.
 * 지금은 로깅 구현으로 흐름만 증명한다.
 */
interface EventPublisher {
    fun publish(event: OutboxEventEntity)
}

@Component
class LoggingEventPublisher : EventPublisher {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(event: OutboxEventEntity) {
        log.info(
            "OUTBOX PUBLISH type={} aggregate={}:{} payload={}",
            event.eventType, event.aggregateType, event.aggregateId, event.payload,
        )
    }
}
