package lemuel.com.pfct.ledger.domain

/**
 * 원장 계정 식별자.
 *
 * 계정 유형 예시:
 * - `investor:{id}`  투자자 예치금 계정
 * - `loan:{id}`      대출 채권 계정
 * - `borrower:{id}`  차주 수신 계정
 * - `platform:fee`   플랫폼 수수료 계정
 */
@JvmInline
value class AccountId(val value: String) {
    init {
        require(value.isNotBlank()) { "AccountId 는 비어 있을 수 없습니다" }
    }
}

@JvmInline
value class TransactionId(val value: String) {
    init {
        require(value.isNotBlank()) { "TransactionId 는 비어 있을 수 없습니다" }
    }
}
