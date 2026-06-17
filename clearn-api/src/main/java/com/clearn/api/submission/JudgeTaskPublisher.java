package com.clearn.api.submission;

import com.clearn.api.config.RabbitConfig;
import com.clearn.common.judge.JudgeTaskMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class JudgeTaskPublisher {
    private final RabbitTemplate rabbitTemplate;

    public JudgeTaskPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(JudgeTaskMessage message) {
        rabbitTemplate.convertAndSend(RabbitConfig.JUDGE_SUBMISSION_QUEUE, message);
    }
}
