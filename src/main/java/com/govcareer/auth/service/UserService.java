package com.govcareer.auth.service;

import com.govcareer.auth.dto.UpdatePasswordRequest;
import com.govcareer.auth.dto.UpdateProfileRequest;
import com.govcareer.auth.dto.UserProfileResponse;
import com.govcareer.auth.entity.User;
import com.govcareer.auth.entity.AuditEventType;
import com.govcareer.auth.repository.UserRepository;
import com.govcareer.auth.security.RefreshTokenService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    public UserProfileResponse getUserProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }

    @Transactional
    @Timed(value = "user.profile.update", description = "Time taken to update user profile")
    public UserProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        // Fetch the managed entity from the DB to ensure we're updating the right instance
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        managedUser.setFirstName(request.firstName());
        managedUser.setLastName(request.lastName());

        userRepository.save(managedUser);
        
        auditService.logEvent(managedUser.getId(), AuditEventType.PROFILE_UPDATED);

        return new UserProfileResponse(
                managedUser.getId(),
                managedUser.getEmail(),
                managedUser.getFirstName(),
                managedUser.getLastName(),
                managedUser.getRole()
        );
    }

    @Transactional
    @Timed(value = "user.password.change", description = "Time taken to change user password")
    public void changePassword(User user, UpdatePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), managedUser.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        managedUser.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(managedUser);

        // Invalidate all refresh tokens for the user
        refreshTokenService.deleteByUserId(managedUser.getId());
        
        auditService.logEvent(managedUser.getId(), AuditEventType.PASSWORD_CHANGED);
    }
}
