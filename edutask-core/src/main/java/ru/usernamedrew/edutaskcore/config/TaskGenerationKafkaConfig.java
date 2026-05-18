package ru.usernamedrew.edutaskcore.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.util.backoff.FixedBackOff;
import ru.usernamedrew.edutaskcommon.kafka.TaskGenerationKafkaProperties;
import ru.usernamedrew.edutaskcommon.kafka.TaskGenerationKafkaTopics;

// TODO В будущем если будут еще сервисы требующие интеграции с Kafka, вынести в отдельный модуль
@Slf4j
@EnableKafka
@Configuration
@EnableConfigurationProperties(TaskGenerationKafkaProperties.class)
public class TaskGenerationKafkaConfig {
    @Bean
    public NewTopic taskGenerationRequestsTopic(TaskGenerationKafkaProperties properties) {
        return TopicBuilder.name(properties.getTopics().getTaskGenerationRequests())
            .partitions(properties.getPartitions())
            .replicas(properties.getReplicationFactor())
            .build();
    }

    @Bean
    public NewTopic taskGenerationResponsesTopic(TaskGenerationKafkaProperties properties) {
        return TopicBuilder.name(properties.getTopics().getTaskGenerationResponses())
            .partitions(properties.getPartitions())
            .replicas(properties.getReplicationFactor())
            .build();
    }

    @Bean
    public NewTopic taskGenerationRequestsDltTopic(TaskGenerationKafkaProperties properties) {
        return TopicBuilder.name(properties.getTopics().getTaskGenerationRequestsDlt())
            .partitions(properties.getPartitions())
            .replicas(properties.getReplicationFactor())
            .build();
    }

    @Bean
    public NewTopic taskGenerationResponsesDltTopic(TaskGenerationKafkaProperties properties) {
        return TopicBuilder.name(properties.getTopics().getTaskGenerationResponsesDlt())
            .partitions(properties.getPartitions())
            .replicas(properties.getReplicationFactor())
            .build();
    }

    @Bean
    public CommonErrorHandler taskGenerationKafkaErrorHandler(
        KafkaTemplate<Object, Object> kafkaTemplate,
        TaskGenerationKafkaProperties properties
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, exception) -> new TopicPartition(
                TaskGenerationKafkaTopics.resolveDltTopic(properties, record.topic()),
                record.partition()
            )
        );
        FixedBackOff backOff = new FixedBackOff(
            properties.getRetry().getBackoff().toMillis(),
            Math.max(0, properties.getRetry().getMaxAttempts() - 1L)
        );
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.setRetryListeners((record, exception, deliveryAttempt) ->
            log.warn(
                "Kafka record processing failed, topic={}, partition={}, offset={}, attempt={}",
                record.topic(),
                record.partition(),
                record.offset(),
                deliveryAttempt,
                exception
            ));
        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
        ConsumerFactory<String, Object> consumerFactory,
        CommonErrorHandler taskGenerationKafkaErrorHandler,
        TaskGenerationKafkaProperties properties
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(properties.getConsumerConcurrency());
        factory.setCommonErrorHandler(taskGenerationKafkaErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}
