package com.clearn.api.config;

import com.clearn.common.enums.JudgeMode;
import com.clearn.common.enums.Language;
import com.clearn.common.judge.JudgeTaskMessage;
import com.clearn.common.json.JsonMappers;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RabbitConfigTest {

    @Autowired
    private MessageConverter messageConverter;

    @Test
    void judgeTaskMessageSerializesCreatedAtAsIsoOffsetString() throws Exception {
        JudgeTaskMessage message = new JudgeTaskMessage(
                10001L,
                3001L,
                Language.C,
                JudgeMode.PRACTICE,
                null,
                OffsetDateTime.parse("2026-06-16T01:55:00+08:00")
        );

        Message amqpMessage = messageConverter.toMessage(message, null);
        String body = new String(amqpMessage.getBody(), StandardCharsets.UTF_8);

        assertThat(body).contains("\"createdAt\":\"2026-06-16T01:55:00+08:00\"");
        assertThat(body).doesNotContain("createdAt\":1");
        assertThat(JsonMappers.objectMapper().readValue(body, JudgeTaskMessage.class).createdAt())
                .isEqualTo(message.createdAt());
    }
}
