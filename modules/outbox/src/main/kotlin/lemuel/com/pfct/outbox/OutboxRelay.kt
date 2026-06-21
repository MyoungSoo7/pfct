package lemuel.com.pfct.outbox

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 아웃박스 릴레이. 발행 후보(미발행·미격리·재시도 대기 종료)를 주기적으로 polling 하여 발행한다.
 *
 * 발행 결과는 **이벤트 단위**로 처리한다 — 한 건이 실패해도 같은 배치의 다른 건은 정상 발행된다.
 *  - 성공: `published_at` 을 찍어 다시 폴링되지 않게 한다.
 *  - 실패: `attempts` 를 늘리고 지수 백오프로 `next_attempt_at` 을 미뤄 재시도를 예약한다(at-least-once).
 *  - 한도([maxAttempts]) 초과: `dead=true` 로 DLQ 격리하고 [DeadLetterPublisher] 로 흘려보낸다 —
 *    독약 메시지(poison message)가 폴링을 영원히 막지 못하게 한다.
 *
 * 후보 조회는 `FOR UPDATE SKIP LOCKED`(ADR-0015)이므로, 릴레이를 **여러 인스턴스**로 띄워도 각자
 * 겹치지 않는 행만 가져가 중복 발행 없이 수평 확장된다 — 단, select→발행→`published_at` 갱신이 한
 * 트랜잭션([publishReadyAt] 의 `@Transactional`) 안에서 일어나야 잠금이 그 사이 유지된다.
 *
 * 테스트에서는 스케줄을 기다리지 않고 [publishReadyAt] 에 명시 시각을 주어 백오프/격리를 결정적으로 검증한다.
 */
@Component
class OutboxRelay(
    private val repository: OutboxJpaRepository,
    private val publisher: EventPublisher,
    private val deadLetters: DeadLetterPublisher,
    @Value("\${pfct.outbox.max-attempts:5}") private val maxAttempts: Int,
    @Value("\${pfct.outbox.backoff-base-ms:1000}") private val backoffBaseMs: Long,
    @Value("\${pfct.outbox.backoff-max-ms:60000}") private val backoffMaxMs: Long,
    @Value("\${pfct.outbox.batch-size:100}") private val batchSize: Int,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${pfct.outbox.relay-delay-ms:2000}")
    @Transactional
    fun publishPending(): Int = publishReadyAt(Instant.now())

    /**
     * [now] 시점 기준 발행 후보를 처리하고, 성공적으로 발행한 건수를 돌려준다.
     * 시각을 인자로 받아 테스트가 백오프 경계를 결정적으로 넘나들 수 있게 한다.
     */
    @Transactional
    fun publishReadyAt(now: Instant): Int {
        val ready = repository.findReadyForPublish(now, PageRequest.of(0, batchSize))
        var published = 0
        for (event in ready) {
            try {
                publisher.publish(event)
                event.publishedAt = now
                published++
            } catch (e: RuntimeException) {
                onFailure(event, now, e)
            }
        }
        // 명시 저장: 호출 컨텍스트에 트랜잭션이 없어도(테스트의 수동 호출) 상태 변경이 확실히 반영되게 한다.
        repository.saveAll(ready)
        return published
    }

    private fun onFailure(event: OutboxEventEntity, now: Instant, e: RuntimeException) {
        event.attempts += 1
        event.lastError = e.message?.take(1000)
        if (event.attempts >= maxAttempts) {
            event.dead = true
            event.nextAttemptAt = null
            runCatching { deadLetters.publish(event) }
                .onFailure { log.error("DLQ 전송 실패 id={} (dead 플래그는 유지)", event.id, it) }
            log.error("아웃박스 이벤트 DLQ 격리 id={} attempts={} lastError={}", event.id, event.attempts, event.lastError)
        } else {
            event.nextAttemptAt = now.plusMillis(backoffMillis(event.attempts))
            log.warn("아웃박스 발행 실패 → 재시도 예약 id={} attempt={} next={}", event.id, event.attempts, event.nextAttemptAt)
        }
    }

    /** 지수 백오프: base × 2^(attempt-1), 상한 [backoffMaxMs]. attempt 는 1부터. */
    private fun backoffMillis(attempt: Int): Long {
        val shift = (attempt - 1).coerceIn(0, 20)
        val delay = backoffBaseMs shl shift
        return if (delay <= 0) backoffMaxMs else delay.coerceAtMost(backoffMaxMs)
    }
}
