package lemuel.com.pfct.outbox

import lemuel.com.pfct.AbstractIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 멀티 인스턴스 릴레이 안전성 — `FOR UPDATE SKIP LOCKED`(ADR-0015) 검증.
 *
 * 여러 워커(=릴레이 인스턴스)가 동시에 같은 아웃박스를 폴링한다. 각 발행을 한 트랜잭션
 * (select→발행→`published_at` 갱신)으로 묶으면, 같은 이벤트를 두 워커가 동시에 가져갈 수 없어야 한다.
 * 발행기는 일부러 짧게 sleep 해 잠금 보유 구간을 넓혀 경합을 유도한다 — 그럼에도 중복 발행이 0이어야 한다.
 */
class OutboxRelayConcurrencyTest @Autowired constructor(
    private val recorder: OutboxRecorder,
    private val repository: OutboxJpaRepository,
    txManager: PlatformTransactionManager,
) : AbstractIntegrationTest() {

    private val tx = TransactionTemplate(txManager)

    @BeforeEach
    fun clean() {
        repository.deleteAll()
    }

    @Test
    fun `여러 릴레이 인스턴스가 동시에 폴링해도 각 이벤트는 정확히 한 번만 발행된다`() {
        val total = 40
        val workers = 6
        repeat(total) { i ->
            recorder.record("Loan", "loan-$i", "LoanDisbursed", """{"seq":$i}""")
        }

        val publishedIds = Collections.synchronizedList(mutableListOf<Long>())
        val publisher = SlowRecordingPublisher(publishedIds)
        val deadLetters = NoopDeadLetters()
        // 워커마다 별도 릴레이 인스턴스(서로 다른 프로세스를 흉내). batchSize 를 작게 줘 경합을 강제.
        val relay = OutboxRelay(repository, publisher, deadLetters, maxAttempts = 5, backoffBaseMs = 1000, backoffMaxMs = 60000, batchSize = 4)

        val pool = Executors.newFixedThreadPool(workers)
        val errors = CopyOnWriteArrayList<Throwable>()
        val start = CountDownLatch(1)
        val deadline = System.currentTimeMillis() + 30_000
        repeat(workers) {
            pool.submit {
                start.await()
                try {
                    while (repository.countByPublishedAtIsNullAndDeadFalse() > 0 && System.currentTimeMillis() < deadline) {
                        val n = tx.execute { relay.publishReadyAt(Instant.now()) } ?: 0
                        if (n == 0) Thread.sleep(1) // 남은 행이 전부 타 워커에 잠긴 상태 → 잠깐 양보.
                    }
                } catch (t: Throwable) {
                    errors.add(t)
                }
            }
        }
        start.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(40, TimeUnit.SECONDS), "워커가 제때 끝나야 한다")

        assertTrue(errors.isEmpty(), "워커 예외: ${errors.firstOrNull()}")
        assertEquals(0, repository.countByPublishedAtIsNullAndDeadFalse(), "모든 이벤트가 발행되어야 한다")
        assertEquals(total, publishedIds.size, "발행 총 횟수 == 이벤트 수 (중복/누락 없음)")
        assertEquals(total, publishedIds.toSet().size, "같은 이벤트가 두 번 발행되면 안 된다")
        // 잠금 쿼리는 트랜잭션 안에서만 — 후보가 비었음을 확인.
        val leftover = tx.execute { repository.findReadyForPublish(Instant.now().plusSeconds(3600), PageRequest.of(0, total)).size }
        assertEquals(0, leftover, "발행 후보가 남으면 안 된다")
    }

    private class SlowRecordingPublisher(private val sink: MutableList<Long>) : EventPublisher {
        override fun publish(event: OutboxEventEntity) {
            Thread.sleep(2) // 잠금 보유 구간을 넓혀 경합 유도.
            sink.add(event.id!!)
        }
    }

    private class NoopDeadLetters : DeadLetterPublisher {
        override fun publish(event: OutboxEventEntity) = Unit
    }
}
