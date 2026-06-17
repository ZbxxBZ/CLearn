package com.clearn.worker.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class WorkerRabbitConfig {
    public static final String JUDGE_SUBMISSION_QUEUE = "judge.submission.queue";

    @Bean
    Queue judgeSubmissionQueue() {
        return QueueBuilder.durable(JUDGE_SUBMISSION_QUEUE).build();
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}
