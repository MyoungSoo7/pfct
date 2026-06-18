package lemuel.com.pfct

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling // 아웃박스 릴레이(@Scheduled) 활성화
class PfctApplication

fun main(args: Array<String>) {
    runApplication<PfctApplication>(*args)
}
