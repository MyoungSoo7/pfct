package lemuel.com.pfct.lending.domain

@JvmInline
value class LoanId(val value: String) {
    init { require(value.isNotBlank()) { "LoanId 는 비어 있을 수 없습니다" } }
}

@JvmInline
value class BorrowerId(val value: String) {
    init { require(value.isNotBlank()) { "BorrowerId 는 비어 있을 수 없습니다" } }
}
