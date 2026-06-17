package lemuel.com.pfct.ledger.domain

import lemuel.com.pfct.common.Money

/**
 * 원장 거래(Aggregate Root).
 *
 * 복식부기의 핵심 불변식 — **차변 합계 == 대변 합계** — 을 생성 시점에 강제한다.
 * 이 불변식 덕분에 시스템 전체에서 "돈이 생기거나 사라지는" 거래는 애초에 만들어질 수 없다.
 * 모든 자금 이동(투자 예치, 대출 실행, 상환, 정산)은 반드시 이 타입을 통해서만 원장에 반영된다.
 */
class LedgerTransaction private constructor(
    val id: TransactionId,
    val description: String,
    val entries: List<JournalEntry>,
) {
    val totalDebit: Money = entries.filter { it.direction == EntryDirection.DEBIT }.sumAmount()
    val totalCredit: Money = entries.filter { it.direction == EntryDirection.CREDIT }.sumAmount()

    companion object {
        /**
         * 검증된 원장 거래를 생성한다.
         * @throws IllegalArgumentException 분개가 2개 미만이거나 차·대변이 일치하지 않을 때
         */
        fun of(
            id: TransactionId,
            description: String,
            entries: List<JournalEntry>,
        ): LedgerTransaction {
            require(entries.size >= 2) {
                "복식부기 거래에는 최소 2개의 분개가 필요합니다 (현재 ${entries.size}개)"
            }
            val debit = entries.filter { it.direction == EntryDirection.DEBIT }.sumAmount()
            val credit = entries.filter { it.direction == EntryDirection.CREDIT }.sumAmount()
            require(debit == credit) {
                "복식부기 불변식 위반 — 차변($debit) ≠ 대변($credit). 돈이 생기거나 사라질 수 없습니다."
            }
            return LedgerTransaction(id, description, entries)
        }
    }
}

private fun List<JournalEntry>.sumAmount(): Money =
    fold(Money.ZERO) { acc, entry -> acc + entry.amount }
