package lemuel.com.pfct.common

import java.time.Instant

/**
 * 모든 도메인 이벤트의 공통 계약.
 *
 * Aggregate 의 상태 변경은 도메인 이벤트를 "반환"하고, 컨텍스트 간 통신은 직접 호출이 아니라
 * 이 이벤트를 통해 이뤄진다(EDA). Phase B에서 이 이벤트가 Outbox → Kafka 로 흘러간다.
 */
interface DomainEvent {
    val occurredAt: Instant
}
