package com.govcareer.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govcareer.auth.dto.LoginRequest;
import com.govcareer.auth.dto.RegisterRequest;
import com.govcareer.auth.repository.UserRepository;
import com.govcareer.auth.repository.EmailVerificationTokenRepository;
import com.govcareer.auth.entity.EmailVerificationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.notNullValue;

@AutoConfigureMockMvc
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    @Test
    void testRegistrationAndLoginFlow() throws Exception {
        // 1. Register a new user
        RegisterRequest registerRequest = new RegisterRequest(
                "Integration",
                "User",
                "integration@govcareer.com",
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", notNullValue()));

        // 2. Fetch the verification token from DB
        EmailVerificationToken token = tokenRepository.findAll().get(0);

        // 3. Verify the email
        mockMvc.perform(post("/api/auth/verify-email")
                .param("token", token.getToken()))
                .andExpect(status().isOk());

        // 4. Login with the registered user
        LoginRequest loginRequest = new LoginRequest(
                "integration@govcareer.com",
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));
    }
}
