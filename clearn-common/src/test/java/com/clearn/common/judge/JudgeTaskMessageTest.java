package com.clearn.common.judge;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearn.common.enums.JudgeMode;
import com.clearn.common.enums.Language;
import com.clearn.common.json.JsonMappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class JudgeTaskMessageTest {

    @Test
    void serializesJudgeTaskMessage() throws Exception {
        ObjectMapper mapper = JsonMappers.objectMapper();
        JudgeTaskMessage message = new JudgeTaskMessage(
                10001L,
                3001L,
                Language.C,
                JudgeMode.PRACTICE,
                null,
                OffsetDateTime.parse("2026-06-16T01:55:00+08:00")
        );

        String json = mapper.writeValueAsString(message);

        assertThat(json).contains("\"submissionId\":10001");
        assertThat(json).contains("\"mode\":\"PRACTICE\"");
        assertThat(json).contains("\"createdAt\":\"2026-06-16T01:55:00+08:00\"");

        JudgeTaskMessage restored = mapper.readValue(json, JudgeTaskMessage.class);
        assertThat(restored.createdAt()).isEqualTo(message.createdAt());
    }
}
