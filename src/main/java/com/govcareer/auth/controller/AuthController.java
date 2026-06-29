package com.govcareer.auth.controller;

import com.govcareer.auth.dto.AuthResponse;
import com.govcareer.auth.dto.ForgotPasswordRequest;
import com.govcareer.auth.dto.GenericResponse;
import com.govcareer.auth.dto.LoginRequest;
import com.govcareer.auth.dto.RegisterRequest;
import com.govcareer.auth.dto.ResendVerificationRequest;
import com.govcareer.auth.dto.ResetPasswordRequest;
import com.govcareer.auth.dto.TokenRefreshRequest;
import com.govcareer.auth.entity.AuditEventType;
import com.govcareer.auth.service.AuditService;
import com.govcareer.auth.service.AuthService;
import com.govcareer.auth.security.RefreshTokenService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    @PostMapping("/register")
    public ResponseEntity<GenericResponse> register(@Valid @RequestBody RegisterRequest request) {
        GenericResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<GenericResponse> verifyEmail(@RequestParam String token) {
        GenericResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<GenericResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        GenericResponse response = authService.resendVerificationEmail(request.email());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Timed(value = "auth.refresh", description = "Time taken to refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        AuthResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Timed(value = "auth.logout", description = "Time taken to logout")
    public ResponseEntity<String> logout(org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.govcareer.auth.security.SecurityUser securityUser) {
            refreshTokenService.deleteByUserId(securityUser.getUser().getId());
            auditService.logEvent(securityUser.getUser().getId(), AuditEventType.LOGOUT);
        }
        return ResponseEntity.ok("Log out successful");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<GenericResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        GenericResponse response = authService.forgotPassword(request.email());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<GenericResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        GenericResponse response = authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(response);
    }
}
