package lemuel.com.pfct.settlement.domain

import lemuel.com.pfct.common.Money
import java.math.BigInteger

/**
 * 비율 분배기 — 금액을 가중치에 비례해 나누되, **분배 합계가 원금과 정확히 일치**하도록 보장한다.
 *
 * 단순히 각자 비율로 나눠 반올림하면 단수(端數) 때문에 합이 1~N원 어긋난다.
 * 여기서는 **최대 잉여(largest remainder) 방식**을 쓴다:
 * 1. 각 몫을 정수로 내림(floor)한다.
 * 2. 남은 금액(원금 − 내림합)을 잉여(나머지)가 큰 순서대로 1원씩 배분한다.
 *
 * → 분배 결과의 합은 항상 입력 금액과 같다. 큰 정수 연산으로 오버플로/오차도 없다.
 */
object ProRataDistributor {

    /**
     * [total] 을 [weights] 비율로 분배한다. 반환 리스트는 입력 순서를 보존하며 합계는 [total] 과 같다.
     * @throws IllegalArgumentException weights 가 비었거나 가중치 합이 0 이하일 때
     */
    fun distribute(total: Money, weights: List<Money>): List<Money> {
        require(weights.isNotEmpty()) { "분배 대상이 비어 있습니다" }

        val totalAmount = total.amount.toBigInteger()
        val weightValues = weights.map { it.amount.toBigInteger() }
        val totalWeight = weightValues.fold(BigInteger.ZERO) { acc, w -> acc + w }
        require(totalWeight.signum() > 0) { "가중치 합은 0보다 커야 합니다" }

        val floors = ArrayList<BigInteger>(weights.size)
        val remainders = ArrayList<BigInteger>(weights.size)
        for (weight in weightValues) {
            val numerator = totalAmount * weight
            floors += numerator / totalWeight
            remainders += numerator % totalWeight
        }

        val distributed = floors.fold(BigInteger.ZERO) { acc, f -> acc + f }
        var leftover = (totalAmount - distributed).toInt() // 0 <= leftover < weights.size

        // 잉여가 큰 순서(동률이면 인덱스 오름차순)로 1원씩 더 준다.
        val order = weights.indices.sortedWith(
            compareByDescending<Int> { remainders[it] }.thenBy { it },
        )
        val result = floors.toMutableList()
        var i = 0
        while (leftover > 0) {
            result[order[i]] = result[order[i]] + BigInteger.ONE
            i++
            leftover--
        }

        return result.map { Money.won(it.toLong()) }
    }
}
