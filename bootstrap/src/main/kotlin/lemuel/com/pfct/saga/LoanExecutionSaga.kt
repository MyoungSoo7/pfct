package lemuel.com.pfct.saga

import lemuel.com.pfct.common.AnnualInterestRate
import lemuel.com.pfct.common.Money
import lemuel.com.pfct.investment.application.ExecuteFundingRoundService
import lemuel.com.pfct.investment.domain.FundingRoundId
import lemuel.com.pfct.lending.application.CreateLoanCommand
import lemuel.com.pfct.lending.application.CreateLoanService
import lemuel.com.pfct.lending.domain.LoanId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * 대출 실행 Saga — **오케스트레이션 기반**.
 *
 * 단계(각각 독립 트랜잭션):
 *  1. 라운드 실행 확정 (investment)
 *  2. 대출 생성        (lending)
 *  3. 지급 + 이벤트     (ledger + outbox)
 *
 * 단일 DB 모놀리스지만, 향후 컨텍스트가 별도 서비스로 분리될 것을 가정해 각 단계를 독립 트랜잭션으로 두고,
 * 후속 단계 실패 시 앞 단계를 **보상 트랜잭션**으로 되돌린다(역순 보상).
 * 오케스트레이터는 트랜잭션을 열지 않는다 — 각 단계가 스스로 커밋한다.
 */
@Service
class LoanExecutionSaga(
    private val executeRound: ExecuteFundingRoundService,
    private val createLoan: CreateLoanService,
    private val disburseLoan: DisburseLoanStep,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(command: ExecuteLoanCommand): LoanExecutionResult {
        val roundId = FundingRoundId(command.roundId)

        // 1단계: 라운드 실행 확정 → 실행 원금 확보 (멱등)
        val principal: Money = executeRound.execute(roundId)

        // 2단계: 대출 생성
        try {
            createLoan.create(
                CreateLoanCommand(
                    loanId = command.loanId,
                    borrowerId = command.borrowerId,
                    principal = principal,
                    annualRate = AnnualInterestRate(command.annualRatePercent),
                    months = command.months,
                ),
            )
        } catch (e: RuntimeException) {
            log.warn("Saga 2단계(대출 생성) 실패 → 1단계 보상", e)
            executeRound.compensate(roundId)
            throw LoanExecutionFailedException(command.loanId, "대출 생성 실패", e)
        }

        // 3단계: 지급 + 아웃박스 이벤트
        try {
            disburseLoan.disburse(command.loanId, command.roundId, command.borrowerId, principal)
        } catch (e: RuntimeException) {
            log.warn("Saga 3단계(지급) 실패 → 2·1단계 보상", e)
            createLoan.cancel(LoanId(command.loanId))
            executeRound.compensate(roundId)
            throw LoanExecutionFailedException(command.loanId, "지급 실패", e)
        }

        return LoanExecutionResult(command.loanId, principal)
    }
}

data class ExecuteLoanCommand(
    val roundId: String,
    val loanId: String,
    val borrowerId: String,
    val annualRatePercent: BigDecimal,
    val months: Int,
)

data class LoanExecutionResult(
    val loanId: String,
    val principal: Money,
)

class LoanExecutionFailedException(loanId: String, reason: String, cause: Throwable) :
    RuntimeException("대출 실행 Saga 실패 (loanId=$loanId): $reason", cause)
