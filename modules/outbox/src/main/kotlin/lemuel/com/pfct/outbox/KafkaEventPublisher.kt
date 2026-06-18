package lemuel.com.pfct.outbox

import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component

/**
 * 아웃박스 이벤트를 Kafka 로 발행하는 [EventPublisher] 구현(@Primary).
 *
 * - 토픽: [TOPIC], 키: aggregateId(같은 애그리거트 이벤트의 순서 보장), 값: payload(JSON)
 * - Spring `Message` 로 보내 헤더(eventType/aggregateType)가 문자열로 올바르게 매핑되게 한다.
 * - 발행 ack 를 동기로 기다린다 — 실패 시 예외가 전파되어 릴레이가 `published_at` 을 찍지 않으므로
 *   다음 폴링에서 재시도된다(at-least-once).
 */
@Component
@Primary
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) : EventPublisher {

    override fun publish(event: OutboxEventEntity) {
        val message = MessageBuilder
            .withPayload(event.payload)
            .setHeader(KafkaHeaders.TOPIC, TOPIC)
            .setHeader(KafkaHeaders.KEY, event.aggregateId)
            .setHeader("eventType", event.eventType)
            .setHeader("aggregateType", event.aggregateType)
            .build()
        kafkaTemplate.send(message).get() // ack 대기(실패 시 예외 → 재시도)
    }

    companion object {
        const val TOPIC = "pfct.outbox"
    }
}
