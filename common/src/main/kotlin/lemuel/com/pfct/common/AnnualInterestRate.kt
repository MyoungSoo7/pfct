package lemuel.com.pfct.common

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 연이자율(%) Value Object.
 *
 * 대부업법상 최고금리(현재 연 20%)를 생성 시점에 강제하여, 불법 금리 데이터가
 * 시스템에 들어오는 것 자체를 막는다. 도메인 규칙을 타입에 가두는 DDD 전술 패턴의 예.
 */
data class AnnualInterestRate(val percent: BigDecimal) {

    init {
        require(percent.signum() >= 0) { "이자율은 음수일 수 없습니다: $percent" }
        require(percent <= LEGAL_MAX_PERCENT) {
            "법정 최고금리(연 $LEGAL_MAX_PERCENT%)를 초과할 수 없습니다: $percent%"
        }
    }

    /** 월이자율(소수) = 연이자율 / 100 / 12. 이자 계산용으로 충분한 정밀도를 유지한다. */
    fun monthlyRate(): BigDecimal =
        percent.divide(BigDecimal(1200), 12, RoundingMode.HALF_UP)

    companion object {
        val LEGAL_MAX_PERCENT: BigDecimal = BigDecimal("20.0")

        fun of(percent: String): AnnualInterestRate = AnnualInterestRate(BigDecimal(percent))
    }
}
