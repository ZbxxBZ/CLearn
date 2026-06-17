package com.clearn.api.config;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "clearn.web.allowed-origins=http://localhost:5173")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestToProtectedApiReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/protected/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentTokenCannotAccessAdminApi() throws Exception {
        String studentToken = loginAndReadToken("student", "password");

        mockMvc.perform(get("/api/admin/ping")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingInternalTokenCannotAccessInternalApi() throws Exception {
        mockMvc.perform(get("/api/internal/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongInternalTokenCannotAccessInternalApi() throws Exception {
        mockMvc.perform(get("/api/internal/ping")
                        .header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validInternalTokenCanAccessInternalApi() throws Exception {
        mockMvc.perform(get("/api/internal/ping")
                        .header("X-Internal-Token", "test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("internal"));
    }

    @Test
    void bearerTokenCannotAccessInternalApi() throws Exception {
        String studentToken = loginAndReadToken("student", "password");

        mockMvc.perform(get("/api/internal/ping")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightAllowsConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/problems")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Access-Control-Allow-Credentials", "true"));
    }

    private String loginAndReadToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(response, "$.data.token");
    }

    @TestConfiguration
    static class TestEndpoints {
        @Bean
        SecurityTestController securityTestController() {
            return new SecurityTestController();
        }
    }

    @RestController
    static class SecurityTestController {
        @GetMapping("/api/protected/ping")
        String protectedPing() {
            return "protected";
        }

        @GetMapping("/api/admin/ping")
        String adminPing() {
            return "admin";
        }

        @GetMapping("/api/internal/ping")
        String internalPing() {
            return "internal";
        }
    }
}
