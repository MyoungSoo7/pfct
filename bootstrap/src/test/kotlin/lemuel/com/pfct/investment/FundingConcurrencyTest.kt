package lemuel.com.pfct.investment

import lemuel.com.pfct.AbstractIntegrationTest
import lemuel.com.pfct.common.Money
import lemuel.com.pfct.investment.adapter.persistence.FundingRoundJpaRepository
import lemuel.com.pfct.investment.application.InvestCommand
import lemuel.com.pfct.investment.application.InvestService
import lemuel.com.pfct.investment.application.OpenFundingRoundCommand
import lemuel.com.pfct.investment.application.OpenFundingRoundService
import lemuel.com.pfct.investment.domain.FundingRoundId
import lemuel.com.pfct.investment.domain.FundingStatus
import lemuel.com.pfct.investment.domain.InvestorId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

/**
 * 정합성 증명 테스트 — "100명이 동시에 막차 투자를 시도해도 모집 한도를 1원도 넘지 않는다".
 *
 * 목표 1,000,000원 라운드에 200개의 스레드가 각각 10,000원을 동시에 투자한다.
 * 비관적 락 + 도메인 불변식이 제대로 동작한다면 정확히 100건만 성공하고 raised == 1,000,000 이어야 한다.
 */
class FundingConcurrencyTest @Autowired constructor(
    private val openFundingRoundService: OpenFundingRoundService,
    private val investService: InvestService,
    private val jpaRepository: FundingRoundJpaRepository,
) : AbstractIntegrationTest() {

    @Test
    fun `200명이 동시에 투자해도 오버펀딩은 발생하지 않는다`() {
        val roundId = "round-concurrency-test"
        val target = 1_000_000L
        val perInvestor = 10_000L
        val threads = 200
        val expectedSuccess = (target / perInvestor).toInt() // 100

        openFundingRoundService.open(OpenFundingRoundCommand(roundId, Money.won(target)))

        val pool = Executors.newFixedThreadPool(32)
        val ready = CountDownLatch(threads)
        val startGun = CountDownLatch(1)
        val success = AtomicInteger(0)
        val rejected = AtomicInteger(0)

        val tasks = (1..threads).map { i ->
            Callable {
                ready.countDown()
                startGun.await() // 모든 스레드가 동시에 출발하도록 정렬
                try {
                    investService.invest(
                        InvestCommand(FundingRoundId(roundId), InvestorId("investor-$i"), Money.won(perInvestor)),
                    )
                    success.incrementAndGet()
                } catch (e: RuntimeException) {
                    rejected.incrementAndGet()
                }
            }
        }

        val futures = tasks.map { pool.submit(it) }
        ready.await(10, TimeUnit.SECONDS)
        startGun.countDown()
        futures.forEach { it.get(60, TimeUnit.SECONDS) }
        pool.shutdown()

        val finalEntity = jpaRepository.findById(roundId).orElseThrow()

        assertEquals(expectedSuccess, success.get(), "성공 건수가 정확히 100이어야 한다")
        assertEquals(threads - expectedSuccess, rejected.get(), "나머지는 모두 거부되어야 한다")
        assertEquals(target, finalEntity.raisedAmount, "모집액은 정확히 목표와 같아야 한다(오버펀딩 0)")
        assertEquals(FundingStatus.FULFILLED, finalEntity.status, "모집이 완료되어야 한다")
    }
}
