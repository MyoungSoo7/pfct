package lemuel.com.pfct

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer

/**
 * 모든 통합 테스트의 베이스. 실제 PostgreSQL 컨테이너를 띄우고(@ServiceConnection 으로 데이터소스 자동 주입),
 * Flyway 마이그레이션 → JPA validate 까지 실제 인프라에 대해 검증한다.
 *
 * **싱글턴 컨테이너 패턴**: 컨테이너를 수동으로 한 번만 start() 하고 명시적으로 stop 하지 않는다.
 * (@Testcontainers + @Container 를 쓰면 첫 테스트 클래스가 끝날 때 컨테이너가 종료되어,
 *  다음 클래스가 죽은 컨테이너에 붙는 문제가 생긴다.) 정리는 Testcontainers 의 Ryuk 리퍼가 JVM 종료 시 수행한다.
 */
@SpringBootTest
abstract class AbstractIntegrationTest {

    companion object {
        @JvmStatic
        @ServiceConnection
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:16-alpine").apply { start() }
    }
}
