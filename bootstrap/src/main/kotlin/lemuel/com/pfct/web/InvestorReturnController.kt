package lemuel.com.pfct.web

import lemuel.com.pfct.readmodel.InvestorReturnViewRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** CQRS 읽기측 — 투자자 수익 조회 전용 엔드포인트. */
@RestController
@RequestMapping("/api/investors")
class InvestorReturnController(
    private val views: InvestorReturnViewRepository,
) {
    @GetMapping("/{investorId}/returns")
    fun returns(@PathVariable investorId: String): InvestorReturnResponse {
        val view = views.findById(investorId).orElse(null)
        return InvestorReturnResponse(
            investorId = investorId,
            totalReturned = view?.totalReturned ?: 0,
            settlementCount = view?.settlementCount ?: 0,
        )
    }
}

data class InvestorReturnResponse(
    val investorId: String,
    val totalReturned: Long,
    val settlementCount: Int,
)
