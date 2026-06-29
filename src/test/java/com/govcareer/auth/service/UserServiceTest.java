package com.govcareer.auth.service;

import com.govcareer.auth.dto.UpdatePasswordRequest;
import com.govcareer.auth.dto.UpdateProfileRequest;
import com.govcareer.auth.dto.UserProfileResponse;
import com.govcareer.auth.entity.Role;
import com.govcareer.auth.entity.User;
import com.govcareer.auth.repository.UserRepository;
import com.govcareer.auth.security.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.govcareer.auth.service.AuditService;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserService userService;

    private User testUser;
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
    }

    @Test
    void getUserProfile_Success() {
        UserProfileResponse response = userService.getUserProfile(testUser);

        assertEquals("test@example.com", response.email());
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals(Role.USER, response.role());
    }

    @Test
    void updateProfile_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Smith");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserProfileResponse response = userService.updateProfile(testUser, request);

        assertEquals("Jane", response.firstName());
        assertEquals("Smith", response.lastName());
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_Success() {
        UpdatePasswordRequest request = new UpdatePasswordRequest(
                "oldPassword", "newPassword", "newPassword");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "hashed_password")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("new_hashed_password");
        
        userService.changePassword(testUser, request);
        
        assertEquals("new_hashed_password", testUser.getPassword());
        verify(userRepository).save(testUser);
        verify(refreshTokenService).deleteByUserId(userId);
    }

    @Test
    void changePassword_PasswordsDoNotMatch_ThrowsException() {
        UpdatePasswordRequest request = new UpdatePasswordRequest(
                "oldPassword", "newPassword", "differentPassword");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> userService.changePassword(testUser, request));
                
        assertEquals("New password and confirm password do not match", exception.getMessage());
        verify(userRepository, times(0)).save(any());
        verify(refreshTokenService, times(0)).deleteByUserId(any());
    }

    @Test
    void changePassword_IncorrectCurrentPassword_ThrowsException() {
        UpdatePasswordRequest request = new UpdatePasswordRequest(
                "wrongPassword", "newPassword", "newPassword");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "hashed_password")).thenReturn(false);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> userService.changePassword(testUser, request));
                
        assertEquals("Current password is incorrect", exception.getMessage());
        verify(userRepository, times(0)).save(any());
        verify(refreshTokenService, times(0)).deleteByUserId(any());
    }
}
