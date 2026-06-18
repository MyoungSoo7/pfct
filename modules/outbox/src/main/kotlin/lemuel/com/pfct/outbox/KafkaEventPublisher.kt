package lemuel.com.pfct.outbox

import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * 아웃박스 이벤트를 Kafka 로 발행하는 [EventPublisher] 구현(@Primary).
 *
 * - 토픽: [TOPIC], 키: aggregateId(같은 애그리거트 이벤트의 순서 보장), 값: payload(JSON)
 * - 발행 ack 를 동기로 기다린다 — 실패하면 예외가 전파되어 릴레이가 `published_at` 을 찍지 않으므로
 *   다음 폴링에서 재시도된다(at-least-once).
 */
@Component
@Primary
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) : EventPublisher {

    override fun publish(event: OutboxEventEntity) {
        val record = ProducerRecord(TOPIC, event.aggregateId, event.payload)
        record.headers().add("eventType", event.eventType.toByteArray())
        record.headers().add("aggregateType", event.aggregateType.toByteArray())
        kafkaTemplate.send(record).get() // ack 대기(실패 시 예외 → 재시도)
    }

    companion object {
        const val TOPIC = "pfct.outbox"
    }
}
