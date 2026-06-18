package lemuel.com.pfct.event

/**
 * 정산 완료 이벤트(아웃박스 → Kafka). CQRS 읽기 모델(투자자 수익) 갱신의 입력이 된다.
 * @property settlementId 정산 자연 키(`settle:{loanId}:{seq}`) — 읽기측 멱등 처리에 사용.
 */
data class RepaymentSettledEvent(
    val settlementId: String,
    val loanId: String,
    val distributions: List<InvestorDistribution>,
)

data class InvestorDistribution(
    val investorId: String,
    val amount: Long,
)
