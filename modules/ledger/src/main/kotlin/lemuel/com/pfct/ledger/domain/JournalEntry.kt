package lemuel.com.pfct.ledger.domain

import lemuel.com.pfct.common.Money

/** 분개 방향. 복식부기에서 모든 금액은 차변(DEBIT) 또는 대변(CREDIT) 중 하나에 기록된다. */
enum class EntryDirection { DEBIT, CREDIT }

/**
 * 분개(分介) 한 줄. "어느 계정에 / 차변·대변 어느 쪽으로 / 얼마를" 기록할지 표현한다.
 * 금액은 항상 양수이며, 방향은 [EntryDirection] 으로 구분한다.
 */
data class JournalEntry(
    val accountId: AccountId,
    val direction: EntryDirection,
    val amount: Money,
) {
    init {
        require(amount.isPositive()) { "분개 금액은 0보다 커야 합니다: $amount" }
    }
}
