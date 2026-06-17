package com.clearn.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultApplicationConfigTest {

    @Test
    void defaultSecurityTokensResolveWithoutExternalEnvironmentVariables() throws Exception {
        MockEnvironment environment = loadMainApplicationYaml();

        assertThat(environment.resolveRequiredPlaceholders("${clearn.security.token-secret}"))
                .isNotBlank();
        assertThat(environment.resolveRequiredPlaceholders("${clearn.security.internal-token}"))
                .isEqualTo("dev-internal-token");
    }

    @Test
    void defaultFlywayConfigBaselinesExistingManuallyCreatedSchema() throws Exception {
        MockEnvironment environment = loadMainApplicationYaml();

        assertThat(environment.resolveRequiredPlaceholders("${spring.flyway.baseline-on-migrate}"))
                .isEqualTo("true");
        assertThat(environment.resolveRequiredPlaceholders("${spring.flyway.baseline-version}"))
                .isEqualTo("1");
    }

    private MockEnvironment loadMainApplicationYaml() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load(
                "applicationConfig",
                new ClassPathResource("application.yml")
        );
        MockEnvironment environment = new MockEnvironment();
        propertySources.forEach(environment.getPropertySources()::addLast);
        return environment;
    }
}
