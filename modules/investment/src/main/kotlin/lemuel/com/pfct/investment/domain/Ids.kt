package lemuel.com.pfct.investment.domain

@JvmInline
value class FundingRoundId(val value: String) {
    init { require(value.isNotBlank()) { "FundingRoundId 는 비어 있을 수 없습니다" } }
}

@JvmInline
value class InvestorId(val value: String) {
    init { require(value.isNotBlank()) { "InvestorId 는 비어 있을 수 없습니다" } }
}
