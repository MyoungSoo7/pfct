package lemuel.com.pfct.outbox

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig {

    @Bean
    fun outboxTopic(): NewTopic =
        TopicBuilder.name(KafkaEventPublisher.TOPIC)
            .partitions(1)
            .replicas(1)
            .build()
}
