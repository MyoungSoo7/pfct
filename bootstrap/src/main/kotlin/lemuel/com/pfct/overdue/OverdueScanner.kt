package lemuel.com.pfct.overdue

import lemuel.com.pfct.lending.application.LoanRepaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 연체 스캐너 — 주기적으로 납기가 지난 미상환 회차를 찾아 연체 처리한다.
 * 각 회차는 [OverdueProcessingService.process] 에서 독립 트랜잭션으로 처리되어, 한 건 실패가 전체를 막지 않는다.
 */
@Component
class OverdueScanner(
    private val repayments: LoanRepaymentRepository,
    private val processor: OverdueProcessingService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${pfct.overdue.scan-delay-ms:60000}")
    fun scan(): Int {
        val today = LocalDate.now()
        val candidates = repayments.findOverdueCandidates(today)
        var processed = 0
        candidates.forEach { entry ->
            if (processor.process(entry.loanId, entry.sequence, today)) processed++
        }
        if (processed > 0) log.info("연체 처리 {}건", processed)
        return processed
    }
}
