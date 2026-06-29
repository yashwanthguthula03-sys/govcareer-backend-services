package com.govcareer.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govcareer.auth.dto.UpdatePasswordRequest;
import com.govcareer.auth.dto.UpdateProfileRequest;
import com.govcareer.auth.dto.UserProfileResponse;
import com.govcareer.auth.entity.Role;
import com.govcareer.auth.entity.User;
import com.govcareer.auth.security.SecurityUser;
import com.govcareer.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private com.govcareer.auth.repository.UserRepository userRepository;

    @MockBean
    private com.govcareer.auth.security.JwtService jwtService;

    private User testUser;
    private SecurityUser securityUser;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .password("hashed_password")
                .firstName("John")
                .lastName("Doe")
                .role(Role.USER)
                .build();
        
        securityUser = new SecurityUser(testUser);
        
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void getCurrentUser_ReturnsProfile() throws Exception {
        UserProfileResponse response = new UserProfileResponse(
                userId, "test@example.com", "John", "Doe", Role.USER);
                
        when(userService.getUserProfile(any(User.class))).thenReturn(response);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void updateProfile_ValidRequest_ReturnsUpdatedProfile() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Smith");
        UserProfileResponse response = new UserProfileResponse(
                userId, "test@example.com", "Jane", "Smith", Role.USER);
                
        when(userService.updateProfile(any(User.class), eq(request))).thenReturn(response);

        mockMvc.perform(put("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"));
    }

    @Test
    void changePassword_ValidRequest_ReturnsSuccess() throws Exception {
        UpdatePasswordRequest request = new UpdatePasswordRequest(
                "oldPassword", "newPassword", "newPassword");

        mockMvc.perform(put("/api/users/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
                
        verify(userService).changePassword(any(User.class), eq(request));
    }
}
