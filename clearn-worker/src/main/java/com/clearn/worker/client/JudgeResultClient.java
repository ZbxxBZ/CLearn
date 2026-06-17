package com.clearn.worker.client;

import com.clearn.common.judge.JudgeFinishRequest;
import com.clearn.common.judge.JudgeSystemErrorRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class JudgeResultClient {
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final RestClient restClient;
    private final String internalToken;

    @Autowired
    public JudgeResultClient(
            RestClient.Builder restClientBuilder,
            @Value("${clearn.worker.internal-api.base-url}") String baseUrl,
            @Value("${clearn.worker.internal-api.token}") String internalToken
    ) {
        this(restClientBuilder.baseUrl(trimTrailingSlash(baseUrl)).build(), internalToken);
    }

    public JudgeResultClient(RestClient restClient, String internalToken) {
        this.restClient = restClient;
        this.internalToken = internalToken;
    }

    public void start(Long submissionId) {
        post("/api/internal/judge/submissions/{id}/start", submissionId, null);
    }

    public void finish(Long submissionId, JudgeFinishRequest request) {
        post("/api/internal/judge/submissions/{id}/finish", submissionId, request);
    }

    public void systemError(Long submissionId, JudgeSystemErrorRequest request) {
        post("/api/internal/judge/submissions/{id}/system-error", submissionId, request);
    }

    private void post(String path, Long submissionId, Object body) {
        RestClient.RequestBodySpec request = restClient.post()
                .uri(path, submissionId)
                .header(INTERNAL_TOKEN_HEADER, internalToken);
        if (body == null) {
            request.retrieve().toBodilessEntity();
            return;
        }
        request.body(body).retrieve().toBodilessEntity();
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null) {
            return null;
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
