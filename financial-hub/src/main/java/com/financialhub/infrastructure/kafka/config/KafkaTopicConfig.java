package com.financialhub.infrastructure.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.transaction-created}")
    private String createdTopic;

    @Value("${app.kafka.topics.transaction-completed}")
    private String completedTopic;

    @Value("${app.kafka.topics.transaction-failed}")
    private String failedTopic;

    @Value("${app.kafka.topics.dlq}")
    private String dlqTopic;

    @Bean
    public NewTopic transactionCreatedTopic() {
        return TopicBuilder.name(createdTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic transactionCompletedTopic() {
        return TopicBuilder.name(completedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic transactionFailedTopic() {
        return TopicBuilder.name(failedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic transactionDlqTopic() {
        return TopicBuilder.name(dlqTopic).partitions(1).replicas(1).build();
    }
}
