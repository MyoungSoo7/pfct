package lemuel.com.pfct.saga

import lemuel.com.pfct.common.Money
import lemuel.com.pfct.ledger.application.RecordLedgerTransactionService
import lemuel.com.pfct.ledger.domain.AccountId
import lemuel.com.pfct.ledger.domain.EntryDirection
import lemuel.com.pfct.ledger.domain.JournalEntry
import lemuel.com.pfct.ledger.domain.LedgerTransaction
import lemuel.com.pfct.ledger.domain.TransactionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 대출 실행 Saga의 지급 단계.
 *
 * 원장 기록과 아웃박스 이벤트 적재를 **하나의 트랜잭션**으로 묶는다(@Transactional → 두 작업이 같은 물리 tx).
 * 따라서 "원장에는 기록됐는데 이벤트는 유실" 같은 이중 쓰기 불일치가 발생하지 않는다.
 *
 * 멱등: 원장 기록이 이미 존재하면(applied=false) 아웃박스 이벤트도 다시 적재하지 않는다.
 */
@Service
class DisburseLoanStep(
    private val ledger: RecordLedgerTransactionService,
    private val outbox: lemuel.com.pfct.outbox.OutboxRecorder,
) {
    @Transactional
    fun disburse(loanId: String, roundId: String, borrowerId: String, principal: Money) {
        val amount = principal.amount.longValueExact()
        val transaction = LedgerTransaction.of(
            id = TransactionId("disburse:$loanId"),
            description = "대출 실행 지급: funding:$roundId → borrower:$borrowerId",
            entries = listOf(
                JournalEntry(AccountId("funding:$roundId"), EntryDirection.CREDIT, principal),
                JournalEntry(AccountId("borrower:$borrowerId"), EntryDirection.DEBIT, principal),
            ),
        )

        val result = ledger.record(transaction)
        if (result.applied) {
            outbox.record(
                aggregateType = "Loan",
                aggregateId = loanId,
                eventType = "LoanDisbursed",
                payload = """{"loanId":"$loanId","borrowerId":"$borrowerId","amount":$amount}""",
            )
        }
    }
}
