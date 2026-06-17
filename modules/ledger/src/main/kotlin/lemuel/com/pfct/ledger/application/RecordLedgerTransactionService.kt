package lemuel.com.pfct.ledger.application

import lemuel.com.pfct.ledger.domain.LedgerTransaction
import lemuel.com.pfct.ledger.domain.TransactionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 원장 거래 기록 유스케이스.
 *
 * **멱등성**: 같은 거래 ID로 두 번 요청이 들어와도(메시지 재전달·클라이언트 재시도) 한 번만 기록된다.
 * 1차 방어는 거래 ID 존재 여부 확인(check-first), 최종 방어는 DB PK(ledger_transaction.id) 유니크 제약이다.
 * 거래 ID는 호출자가 자금 이동의 자연 키(예: `disburse:{loanId}`)로 부여하여 멱등 키 역할을 한다.
 */
@Service
class RecordLedgerTransactionService(
    private val ledger: LedgerRepository,
) {
    @Transactional
    fun record(transaction: LedgerTransaction): RecordLedgerResult {
        if (ledger.exists(transaction.id)) {
            return RecordLedgerResult(transaction.id, applied = false)
        }
        ledger.append(transaction)
        return RecordLedgerResult(transaction.id, applied = true)
    }
}

/** @property applied 이번 호출로 실제 기록되었으면 true, 이미 존재해 건너뛰었으면 false. */
data class RecordLedgerResult(
    val transactionId: TransactionId,
    val applied: Boolean,
)
