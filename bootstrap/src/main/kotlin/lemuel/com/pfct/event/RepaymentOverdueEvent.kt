package lemuel.com.pfct.event

/** 상환 회차 연체 발생 이벤트(아웃박스 → Kafka). */
data class RepaymentOverdueEvent(
    val loanId: String,
    val sequence: Int,
    val overdueAmount: Long,
    val lateFee: Long,
    val dueDate: String,
)
