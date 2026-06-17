package com.clearn.worker.judge;

import com.clearn.common.judge.JudgeTaskMessage;
import com.clearn.worker.config.WorkerRabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class JudgeTaskListener {
    private final JudgeCoordinator coordinator;

    public JudgeTaskListener(JudgeCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @RabbitListener(queues = WorkerRabbitConfig.JUDGE_SUBMISSION_QUEUE)
    public void handle(JudgeTaskMessage message) {
        coordinator.judge(message);
    }
}
