package lemuel.com.pfct.readmodel

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository

/**
 * CQRS 읽기 모델 — 투자자별 누적 수익(정산으로 돌려받은 금액). 쓰기 모델(원장/정산)과 분리된
 * 비정규화 조회 전용 테이블로, 정산 이벤트를 구독해 갱신된다.
 */
@Entity
@Table(name = "investor_return_view")
class InvestorReturnView(
    @Id
    @Column(name = "investor_id", nullable = false, length = 64)
    val investorId: String,

    @Column(name = "total_returned", nullable = false)
    var totalReturned: Long,

    @Column(name = "settlement_count", nullable = false)
    var settlementCount: Int,
)

interface InvestorReturnViewRepository : JpaRepository<InvestorReturnView, String>

/** 이미 처리한 이벤트(정산 ID) 기록 — 읽기측 멱등 처리(at-least-once 대비)용. */
@Entity
@Table(name = "processed_event")
class ProcessedEvent(
    @Id
    @Column(name = "event_id", nullable = false, length = 128)
    val eventId: String,
)

interface ProcessedEventRepository : JpaRepository<ProcessedEvent, String>
