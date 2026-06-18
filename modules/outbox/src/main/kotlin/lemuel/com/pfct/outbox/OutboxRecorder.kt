package lemuel.com.pfct.outbox

import org.springframework.stereotype.Component
import java.time.Instant

/**
 * 아웃박스에 이벤트를 적재한다. **반드시 호출자의 트랜잭션 안에서 실행되어야 한다** —
 * 그래야 상태 변경(예: 원장 기록)과 이벤트 적재가 원자적으로 함께 커밋된다(이중 쓰기 문제 해결).
 */
@Component
class OutboxRecorder(
    private val repository: OutboxJpaRepository,
) {
    fun record(aggregateType: String, aggregateId: String, eventType: String, payload: String) {
        repository.save(
            OutboxEventEntity(
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                payload = payload,
                createdAt = Instant.now(),
            ),
        )
    }
}
