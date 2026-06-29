package com.govcareer.auth.controller;

import com.govcareer.auth.dto.UpdatePasswordRequest;
import com.govcareer.auth.dto.UpdateProfileRequest;
import com.govcareer.auth.dto.UserProfileResponse;
import com.govcareer.auth.security.SecurityUser;
import com.govcareer.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Operations pertaining to user profile and identity management")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile", description = "Retrieves the authenticated user's profile details.")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal SecurityUser securityUser) {
        return ResponseEntity.ok(userService.getUserProfile(securityUser.getUser()));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update User Profile", description = "Updates the authenticated user's profile (first name and last name).")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        UserProfileResponse response = userService.updateProfile(securityUser.getUser(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/password")
    @Operation(summary = "Change Password", description = "Changes the user's password and invalidates all active sessions.")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody UpdatePasswordRequest request) {
        
        userService.changePassword(securityUser.getUser(), request);
        return ResponseEntity.ok("Password updated successfully");
    }
}
