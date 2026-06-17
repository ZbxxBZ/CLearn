package com.clearn.api.staticapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FrontendSeparationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void apiModuleDoesNotRegisterFrontendShellController() {
        assertThat(applicationContext.containsBeanDefinition("rootPageController")).isFalse();
    }

    @Test
    void apiModuleDoesNotContainBundledFrontendAssets() {
        assertThat(Path.of("src/main/resources/static")).doesNotExist();
        assertThat(new ClassPathResource("static/index.html").exists()).isFalse();
        assertThat(new ClassPathResource("static/assets/index.js").exists()).isFalse();
        assertThat(new ClassPathResource("static/assets/index.css").exists()).isFalse();
    }

    @Test
    void rootAndAssetPathsAreNotPublicFrontendEntrypoints() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/assets/index.js"))
                .andExpect(status().isUnauthorized());
    }
}
