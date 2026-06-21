package lemuel.com.pfct.outbox

import lemuel.com.pfct.AbstractIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 아웃박스 재시도/백오프 + DLQ 격리를 **결정적으로** 검증한다.
 * 실패하는 [EventPublisher] 와 캡처용 [DeadLetterPublisher] 를 직접 주입한 릴레이를 만들고,
 * `publishReadyAt(now)` 에 명시 시각을 주어 백오프 경계를 넘나든다(스케줄/실시간 의존 제거).
 *
 * 후보 조회가 `FOR UPDATE SKIP LOCKED`(ADR-0015)라 트랜잭션이 필요하므로,
 * 발행 호출은 [TransactionTemplate] 로 감싼다 — 실제 릴레이의 `@Transactional` 경계를 흉내낸다.
 */
class OutboxDlqIntegrationTest @Autowired constructor(
    private val recorder: OutboxRecorder,
    private val repository: OutboxJpaRepository,
    txManager: PlatformTransactionManager,
) : AbstractIntegrationTest() {

    private val tx = TransactionTemplate(txManager)
    private val t0: Instant = Instant.parse("2026-06-20T00:00:00Z")

    /** 실제 릴레이 경로처럼 한 트랜잭션 안에서 발행을 수행한다. */
    private fun OutboxRelay.publish(now: Instant): Int = tx.execute { publishReadyAt(now) } ?: 0

    @BeforeEach
    fun clean() {
        repository.deleteAll() // 다른 테스트가 남긴 이벤트와 격리.
    }

    @Test
    fun `발행이 반복 실패하면 백오프로 재시도하다가 한도 초과 시 DLQ로 격리된다`() {
        recorder.record("Loan", "loan-dlq", "LoanDisbursed", """{"loanId":"loan-dlq"}""")
        val dlq = CapturingDeadLetters()
        val relay = relayWith(BoomPublisher(), dlq, maxAttempts = 3)

        // 1차 시도(t0) → 실패, attempts=1, next=t0+1s
        assertEquals(0, relay.publish(t0))
        assertEquals(1, only().attempts)
        assertFalse(only().dead)

        // 백오프 게이팅: next(t0+1s) 이전 시각이면 후보로 잡히지 않아 재시도하지 않는다.
        assertEquals(0, relay.publish(t0))
        assertEquals(1, only().attempts)

        // 2차 시도(t0+2s) → 실패, attempts=2, next=(t0+2s)+2s
        assertEquals(0, relay.publish(t0.plusSeconds(2)))
        assertEquals(2, only().attempts)
        assertFalse(only().dead)

        // 3차 시도(t0+10s) → 한도(3) 도달 → DLQ 격리
        assertEquals(0, relay.publish(t0.plusSeconds(10)))
        val dead = only()
        assertEquals(3, dead.attempts)
        assertTrue(dead.dead, "한도 초과 이벤트는 dead 로 격리되어야 한다")
        assertNotNull(dead.lastError)

        // DLQ 발행 1회 + 운영 조회에 노출 + 더는 발행 후보가 아님.
        assertEquals(1, dlq.events.size)
        assertEquals(1, repository.findByDeadTrueOrderByIdAsc().size)
        assertEquals(0, repository.countByPublishedAtIsNullAndDeadFalse(), "격리된 이벤트는 발행 후보가 아니다")
    }

    @Test
    fun `재시도 도중 발행이 성공하면 발행 완료로 마감되고 DLQ로 가지 않는다`() {
        recorder.record("Loan", "loan-recover", "LoanDisbursed", """{"loanId":"loan-recover"}""")
        val dlq = CapturingDeadLetters()

        // 1차 실패로 재시도 예약.
        relayWith(BoomPublisher(), dlq, maxAttempts = 3).publish(t0)
        assertEquals(1, only().attempts)

        // 백오프 종료 후 정상 발행되면 published_at 이 찍히고 dead 가 아니다.
        val ok = RecordingPublisher()
        val published = relayWith(ok, dlq, maxAttempts = 3).publish(t0.plusSeconds(2))
        assertEquals(1, published)
        assertEquals(1, ok.count)

        val saved = only()
        assertNotNull(saved.publishedAt, "성공 발행은 published_at 이 찍혀야 한다")
        assertFalse(saved.dead)
        assertTrue(dlq.events.isEmpty(), "정상 발행 건은 DLQ로 가지 않는다")
    }

    // --- helpers ---

    private fun only() = repository.findAll().single()

    private fun relayWith(publisher: EventPublisher, dlq: DeadLetterPublisher, maxAttempts: Int) =
        OutboxRelay(
            repository = repository,
            publisher = publisher,
            deadLetters = dlq,
            maxAttempts = maxAttempts,
            backoffBaseMs = 1000,
            backoffMaxMs = 60000,
            batchSize = 100,
        )

    private class BoomPublisher : EventPublisher {
        override fun publish(event: OutboxEventEntity) = throw RuntimeException("발행 실패(테스트)")
    }

    private class RecordingPublisher : EventPublisher {
        var count = 0
        override fun publish(event: OutboxEventEntity) {
            count++
        }
    }

    private class CapturingDeadLetters : DeadLetterPublisher {
        val events = CopyOnWriteArrayList<OutboxEventEntity>()
        override fun publish(event: OutboxEventEntity) {
            events.add(event)
        }
    }
}
