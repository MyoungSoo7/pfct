package lemuel.com.pfct.common

import java.math.BigDecimal

/**
 * 원화(KRW) 금액을 표현하는 Value Object.
 *
 * 금융 시스템에서 돈을 Double/Float 로 다루면 부동소수점 오차로 1원이 사라지거나 생긴다.
 * 이 타입은 내부적으로 [BigDecimal] 정수(원 단위, scale=0)만 허용하여 그런 오류를 타입 수준에서 차단한다.
 */
@JvmInline
value class Money private constructor(val amount: BigDecimal) : Comparable<Money> {

    companion object {
        val ZERO: Money = Money(BigDecimal.ZERO.setScale(0))

        /** 원 단위 정수로 생성한다. (예: `Money.won(1_000_000)`) */
        fun won(value: Long): Money = Money(BigDecimal.valueOf(value).setScale(0))

        /** BigDecimal 로 생성하되, 원 미만(소수)이 있으면 거부한다. */
        fun won(value: BigDecimal): Money {
            require(value.stripTrailingZeros().scale() <= 0) {
                "원화는 1원 미만 단위를 가질 수 없습니다: $value"
            }
            return Money(value.setScale(0))
        }
    }

    operator fun plus(other: Money): Money = Money(amount + other.amount)
    operator fun minus(other: Money): Money = Money(amount - other.amount)
    operator fun times(factor: Int): Money = Money(amount * BigDecimal.valueOf(factor.toLong()))

    fun isPositive(): Boolean = amount.signum() > 0
    fun isNegative(): Boolean = amount.signum() < 0
    fun isZero(): Boolean = amount.signum() == 0

    override fun compareTo(other: Money): Int = amount.compareTo(other.amount)

    override fun toString(): String = "${amount.toPlainString()}원"
}
