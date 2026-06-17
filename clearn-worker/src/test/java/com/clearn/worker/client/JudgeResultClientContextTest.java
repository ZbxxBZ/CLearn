package com.clearn.worker.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class JudgeResultClientContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ClientAndConfigScan.class)
            .withPropertyValues(
                    "clearn.worker.internal-api.base-url=http://localhost:8080",
                    "clearn.worker.internal-api.token=test-internal-token"
            );

    @Test
    void createsJudgeResultClientWithRestClientBuilderFromWorkerConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(RestClient.Builder.class);
            assertThat(context).hasSingleBean(JudgeResultClient.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackages = {
            "com.clearn.worker.client",
            "com.clearn.worker.config"
    })
    static class ClientAndConfigScan {
    }
}
