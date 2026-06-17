package com.clearn.worker.config;

import com.clearn.common.enums.JudgeMode;
import com.clearn.common.enums.Language;
import com.clearn.common.judge.JudgeTaskMessage;
import com.clearn.common.json.JsonMappers;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerRabbitConfigTest {

    @Test
    void declaresDurableJudgeSubmissionQueue() {
        WorkerRabbitConfig config = new WorkerRabbitConfig();

        Queue queue = config.judgeSubmissionQueue();

        assertThat(queue.getName()).isEqualTo("judge.submission.queue");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void converterSerializesCreatedAtAsIsoOffsetStringReadableByCommonMapper() throws Exception {
        WorkerRabbitConfig config = new WorkerRabbitConfig();
        MessageConverter converter = config.rabbitMessageConverter();
        JudgeTaskMessage message = new JudgeTaskMessage(
                10001L,
                3001L,
                Language.C,
                JudgeMode.PRACTICE,
                null,
                OffsetDateTime.parse("2026-06-16T01:55:00+08:00")
        );

        Message amqpMessage = converter.toMessage(message, null);
        String body = new String(amqpMessage.getBody(), StandardCharsets.UTF_8);

        assertThat(body).contains("\"createdAt\":\"2026-06-16T01:55:00+08:00\"");
        assertThat(JsonMappers.objectMapper().readValue(body, JudgeTaskMessage.class).createdAt())
                .isEqualTo(message.createdAt());
    }
}
