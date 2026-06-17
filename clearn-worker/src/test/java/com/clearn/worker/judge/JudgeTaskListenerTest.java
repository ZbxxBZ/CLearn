package com.clearn.worker.judge;

import com.clearn.common.enums.JudgeMode;
import com.clearn.common.enums.Language;
import com.clearn.common.judge.JudgeTaskMessage;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class JudgeTaskListenerTest {

    @Test
    void handleDelegatesMessageToCoordinator() {
        JudgeCoordinator coordinator = mock(JudgeCoordinator.class);
        JudgeTaskListener listener = new JudgeTaskListener(coordinator);
        JudgeTaskMessage message = new JudgeTaskMessage(
                10001L,
                3001L,
                Language.C,
                JudgeMode.PRACTICE,
                null,
                OffsetDateTime.parse("2026-06-16T01:55:00+08:00")
        );

        listener.handle(message);

        verify(coordinator).judge(message);
        verifyNoMoreInteractions(coordinator);
    }
}
