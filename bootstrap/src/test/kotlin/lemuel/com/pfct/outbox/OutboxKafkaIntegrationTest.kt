package lemuel.com.pfct.outbox

import lemuel.com.pfct.AbstractIntegrationTest
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

/**
 * 아웃박스 → Kafka 발행 → 구독까지 실제 Kafka 컨테이너로 검증한다.
 * 아웃박스에 이벤트를 적재하고 릴레이를 돌리면, 테스트 컨슈머가 Kafka 에서 같은 payload 를 받아야 한다.
 */
@Import(OutboxKafkaIntegrationTest.TestKafkaConsumer::class)
class OutboxKafkaIntegrationTest @Autowired constructor(
    private val outboxRecorder: OutboxRecorder,
    private val relay: OutboxRelay,
    private val consumer: TestKafkaConsumer,
) : AbstractIntegrationTest() {

    @Test
    fun `아웃박스 이벤트가 Kafka 로 발행되어 컨슈머에 도달한다`() {
        outboxRecorder.record(
            aggregateType = "Loan",
            aggregateId = "loan-kafka",
            eventType = "LoanDisbursed",
            payload = """{"loanId":"loan-kafka","amount":1000000}""",
        )

        val published = relay.publishPending()
        assertTrue(published >= 1, "릴레이가 최소 1건을 발행해야 한다")

        // 컨슈머가 Kafka 에서 해당 이벤트를 수신할 때까지 대기.
        await().atMost(15, TimeUnit.SECONDS).until {
            consumer.received().any { it.contains("loan-kafka") }
        }
    }

    @Component
    class TestKafkaConsumer {
        private val messages = CopyOnWriteArrayList<String>()

        @KafkaListener(topics = [KafkaEventPublisher.TOPIC], groupId = "test-consumer")
        fun consume(payload: String) {
            messages.add(payload)
        }

        fun received(): List<String> = messages
    }
}
