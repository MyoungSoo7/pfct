package lemuel.com.pfct.investment.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import lemuel.com.pfct.investment.domain.FundingStatus

/**
 * 펀딩 라운드 JPA 엔티티. 도메인 모델([lemuel.com.pfct.investment.domain.FundingRound])과 분리되어,
 * 영속화 관심사(컬럼 매핑 등)가 도메인을 오염시키지 않도록 한다.
 *
 * 금액은 원(KRW) 단위 정수이므로 `bigint(Long)` 으로 저장한다.
 */
@Entity
@Table(name = "funding_round")
class FundingRoundJpaEntity(
    @Id
    @Column(name = "id", nullable = false, length = 64)
    val id: String,

    @Column(name = "target_amount", nullable = false)
    val targetAmount: Long,

    @Column(name = "raised_amount", nullable = false)
    var raisedAmount: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: FundingStatus,
)
