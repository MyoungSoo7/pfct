package lemuel.com.pfct.readmodel

import com.fasterxml.jackson.databind.ObjectMapper
import lemuel.com.pfct.event.RepaymentSettledEvent
import lemuel.com.pfct.outbox.KafkaEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 정산 이벤트를 구독해 투자자 수익 읽기 모델을 갱신하는 CQRS 프로젝터.
 *
 * **멱등**: Kafka 는 at-least-once 라 같은 이벤트가 재전달될 수 있으므로, 정산 ID를 `processed_event` 에
 * 기록해 두 번 반영되지 않게 한다(중복이면 건너뜀).
 */
@Component
class InvestorReturnsProjector(
    private val views: InvestorReturnViewRepository,
    private val processedEvents: ProcessedEventRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [KafkaEventPublisher.TOPIC], groupId = "cqrs-investor-returns")
    @Transactional
    fun on(
        @Payload payload: String,
        @Header(name = "eventType", required = false) eventType: String?,
    ) {
        if (eventType != "RepaymentSettled") return // 이 프로젝터는 정산 이벤트만 처리

        val event = objectMapper.readValue(payload, RepaymentSettledEvent::class.java)
        if (processedEvents.existsById(event.settlementId)) {
            log.debug("이미 처리한 정산 이벤트 — 건너뜀: {}", event.settlementId)
            return
        }

        event.distributions.forEach { distribution ->
            val view = views.findById(distribution.investorId).orElseGet {
                InvestorReturnView(distribution.investorId, totalReturned = 0, settlementCount = 0)
            }
            view.totalReturned += distribution.amount
            view.settlementCount += 1
            views.save(view)
        }
        processedEvents.save(ProcessedEvent(event.settlementId))
    }
}
