package lemuel.com.pfct.ledger.application

import lemuel.com.pfct.common.Money
import lemuel.com.pfct.ledger.domain.AccountId
import lemuel.com.pfct.ledger.domain.LedgerTransaction
import lemuel.com.pfct.ledger.domain.TransactionId

/**
 * 원장 영속화 포트. 원장은 **append-only** 다 — 한 번 기록된 거래는 수정/삭제되지 않는다.
 * 잔액은 분개(journal entry)의 합으로 도출한다.
 */
interface LedgerRepository {

    /** 해당 거래 ID가 이미 기록되었는지 여부(멱등성 판단용). */
    fun exists(id: TransactionId): Boolean

    /** 검증된 원장 거래를 추가한다(헤더 + 분개들). */
    fun append(transaction: LedgerTransaction)

    /**
     * 계정 잔액 = Σ(차변) − Σ(대변). (자산 계정 관점의 부호 규약)
     * 균형 잡힌 거래만 기록되므로 전 계정 잔액의 총합은 항상 0이다.
     */
    fun balanceOf(accountId: AccountId): Money
}
