package com.shuld.jac.authservice.integration;

import com.shuld.jac.authservice.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    private String registerPayload(Long userId, String login, String password, Role role) {
        return """
                {"userId": %d, "login": "%s", "password": "%s", "role": "%s"}
                """.formatted(userId, login, password, role);
    }

    private void register(Long userId, String login, String password, Role role) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(userId, login, password, role)))
                .andExpect(status().isCreated());
    }

    @Test
    void register_persistsHashedPassword() throws Exception {
        register(1L, "john.doe", "plaintext123", Role.USER);

        var credential = credentialRepository.findByLogin("john.doe").orElseThrow();
        assertThat(credential.getPasswordHash()).isNotEqualTo("plaintext123");
        assertThat(credential.getPasswordHash()).startsWith("$2");
        assertThat(credential.getUserId()).isEqualTo(1L);
        assertThat(credential.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void register_duplicateLogin_returnsConflict() throws Exception {
        register(1L, "john.doe", "plaintext123", Role.USER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(2L, "john.doe", "otherpassword", Role.USER)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalidPayload_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_returnsAccessAndRefreshTokens() throws Exception {
        register(1L, "john.doe", "plaintext123", Role.USER);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"john.doe\", \"password\": \"plaintext123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        register(1L, "john.doe", "plaintext123", Role.USER);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"john.doe\", \"password\": \"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownLogin_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"nobody\", \"password\": \"whatever\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_validRefreshToken_returnsNewTokens() throws Exception {
        register(1L, "john.doe", "plaintext123", Role.USER);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"john.doe\", \"password\": \"plaintext123\"}"))
                .andReturn();
        Map<String, Object> tokens = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + tokens.get("refreshToken") + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void refresh_accessTokenUsedAsRefreshToken_returnsUnauthorized() throws Exception {
        register(1L, "john.doe", "plaintext123", Role.USER);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"john.doe\", \"password\": \"plaintext123\"}"))
                .andReturn();
        Map<String, Object> tokens = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + tokens.get("accessToken") + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validate_validAccessToken_returnsClaims() throws Exception {
        register(1L, "john.doe", "plaintext123", Role.ADMIN);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"john.doe\", \"password\": \"plaintext123\"}"))
                .andReturn();
        Map<String, Object> tokens = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);

        mockMvc.perform(post("/api/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + tokens.get("accessToken") + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void validate_garbageToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"not-a-real-token\"}"))
                .andExpect(status().isUnauthorized());
    }
}
