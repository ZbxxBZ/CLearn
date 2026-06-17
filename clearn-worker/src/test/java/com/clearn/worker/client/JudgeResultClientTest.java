package com.clearn.worker.client;

import com.clearn.common.enums.SubmissionStatus;
import com.clearn.common.judge.JudgeFinishRequest;
import com.clearn.common.judge.JudgeSystemErrorRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class JudgeResultClientTest {
    private MockRestServiceServer server;
    private JudgeResultClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://api.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new JudgeResultClient(builder.build(), "test-internal-token");
    }

    @Test
    void startPostsToInternalApiWithTokenHeader() {
        server.expect(once(), requestTo("http://api.test/api/internal/judge/submissions/10001/start"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Token", "test-internal-token"))
                .andRespond(withSuccess());

        client.start(10001L);

        server.verify();
    }

    @Test
    void finishPostsRequestBodyWithTokenHeader() {
        server.expect(once(), requestTo("http://api.test/api/internal/judge/submissions/10001/finish"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Token", "test-internal-token"))
                .andExpect(content().string(containsString("\"status\":\"AC\"")))
                .andRespond(withSuccess());
        JudgeFinishRequest request = new JudgeFinishRequest(
                SubmissionStatus.AC,
                100,
                5,
                5,
                12L,
                256L,
                null
        );

        client.finish(10001L, request);

        server.verify();
    }

    @Test
    void systemErrorPostsRequestBodyWithTokenHeader() {
        server.expect(once(), requestTo("http://api.test/api/internal/judge/submissions/10001/system-error"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Token", "test-internal-token"))
                .andExpect(content().string(containsString("worker failed to load submission")))
                .andRespond(withSuccess());
        JudgeSystemErrorRequest request = new JudgeSystemErrorRequest("worker failed to load submission");

        client.systemError(10001L, request);

        server.verify();
    }

    @Test
    void productionConstructorIsExplicitlyAutowired() {
        Constructor<?> productionConstructor = Arrays.stream(JudgeResultClient.class.getConstructors())
                .filter(constructor -> constructor.getParameterCount() == 3)
                .findFirst()
                .orElseThrow();

        assertThat(productionConstructor.isAnnotationPresent(Autowired.class)).isTrue();
    }
}
