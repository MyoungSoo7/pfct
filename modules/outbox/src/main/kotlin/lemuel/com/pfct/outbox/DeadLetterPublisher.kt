package lemuel.com.pfct.outbox

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component

/**
 * 재시도 한도를 넘긴 이벤트를 데드레터(DLQ)로 보낸다. 정상 토픽을 막지 않도록 격리하는 출구다.
 * DB 의 `dead` 플래그가 권위 있는 격리 기록이고, 이 발행은 운영 파이프라인(알림/재처리)을 위한 부가 전송이다.
 */
interface DeadLetterPublisher {
    fun publish(event: OutboxEventEntity)
}

/**
 * DLQ 를 Kafka 토픽([TOPIC])으로 발행하는 구현. 원래 이벤트의 페이로드/키를 그대로 두고,
 * 격리 사유(`deadLetterReason`)와 시도 횟수(`attempts`)를 헤더로 덧붙인다.
 */
@Component
class KafkaDeadLetterPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) : DeadLetterPublisher {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(event: OutboxEventEntity) {
        val message = MessageBuilder
            .withPayload(event.payload)
            .setHeader(KafkaHeaders.TOPIC, TOPIC)
            .setHeader(KafkaHeaders.KEY, event.aggregateId)
            .setHeader("eventType", event.eventType)
            .setHeader("aggregateType", event.aggregateType)
            .setHeader("attempts", event.attempts.toString())
            .setHeader("deadLetterReason", event.lastError ?: "unknown")
            .build()
        kafkaTemplate.send(message).get() // ack 대기
        log.warn("아웃박스 이벤트 DLQ 전송 id={} type={} attempts={}", event.id, event.eventType, event.attempts)
    }

    companion object {
        const val TOPIC = "pfct.outbox.dlq"
    }
}
