package com.govcareer.auth.service;

import com.govcareer.auth.dto.AuthResponse;
import com.govcareer.auth.dto.GenericResponse;
import com.govcareer.auth.dto.LoginRequest;
import com.govcareer.auth.dto.RegisterRequest;
import com.govcareer.auth.entity.AuditEventType;
import com.govcareer.auth.entity.EmailVerificationToken;
import com.govcareer.auth.entity.PasswordResetToken;
import com.govcareer.auth.entity.RefreshToken;
import com.govcareer.auth.entity.Role;
import com.govcareer.auth.entity.User;
import com.govcareer.auth.exception.TokenRefreshException;
import com.govcareer.auth.repository.EmailVerificationTokenRepository;
import com.govcareer.auth.repository.PasswordResetTokenRepository;
import com.govcareer.auth.repository.UserRepository;
import com.govcareer.auth.security.JwtService;
import com.govcareer.auth.security.RefreshTokenService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final EmailService emailService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    @Timed(value = "auth.register", description = "Time taken to register a user")
    @Transactional
    public GenericResponse register(RegisterRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(request.email());
        if (existingUser.isPresent()) {
            // Prevent account enumeration by returning the same generic response
            return new GenericResponse("Registration successful. Please check your email to verify your account.");
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .build();

        user = userRepository.save(user);

        String tokenStr = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(tokenStr)
                .user(user)
                .expiryDate(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        emailVerificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), tokenStr);

        auditService.logEvent(user.getId(), AuditEventType.REGISTRATION_SUCCESS);
        return new GenericResponse("Registration successful. Please check your email to verify your account.");
    }

    @Transactional
    public GenericResponse verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (Instant.now().isAfter(verificationToken.getExpiryDate())) {
            throw new IllegalArgumentException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        emailVerificationTokenRepository.delete(verificationToken);

        auditService.logEvent(user.getId(), AuditEventType.EMAIL_VERIFIED);
        return new GenericResponse("Email successfully verified.");
    }

    @Transactional
    public GenericResponse resendVerificationEmail(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                emailVerificationTokenRepository.deleteByUserId(user.getId());

                String tokenStr = UUID.randomUUID().toString();
                EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                        .token(tokenStr)
                        .user(user)
                        .expiryDate(Instant.now().plus(24, ChronoUnit.HOURS))
                        .build();
                emailVerificationTokenRepository.save(verificationToken);

                emailService.sendVerificationEmail(user.getEmail(), tokenStr);
            }
        });

        return new GenericResponse("If your email is registered and unverified, a new verification link has been sent.");
    }

    @Timed(value = "auth.login", description = "Time taken to login")
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            org.springframework.security.core.Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            com.govcareer.auth.security.SecurityUser securityUser = (com.govcareer.auth.security.SecurityUser) authentication.getPrincipal();
            User user = securityUser.getUser();

            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);

            var jwtToken = jwtService.generateToken(user);
            var refreshToken = refreshTokenService.createRefreshToken(user.getId());

            auditService.logEvent(user.getId(), AuditEventType.LOGIN_SUCCESS);

            return new AuthResponse(jwtToken, refreshToken.getToken());
        } catch (Exception e) {
            handleFailedLogin(request.email());
            throw e;
        }
    }

    private void handleFailedLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            
            auditService.logEvent(user.getId(), AuditEventType.LOGIN_FAILURE);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(Instant.now().plus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES));
                auditService.logEvent(user.getId(), AuditEventType.ACCOUNT_LOCKED);
            }
            userRepository.save(user);
        });
    }

    @Transactional
    public GenericResponse forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUserId(user.getId());

            String tokenStr = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(tokenStr)
                    .user(user)
                    .expiryDate(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();
            
            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(user.getEmail(), tokenStr);
            
            auditService.logEvent(user.getId(), AuditEventType.PASSWORD_RESET_REQUESTED);
        });

        // Always return generic success to prevent email enumeration
        return new GenericResponse("If an account exists for this email, a reset link has been sent.");
    }

    @Transactional
    public GenericResponse resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token"));

        if (Instant.now().isAfter(resetToken.getExpiryDate())) {
            throw new IllegalArgumentException("Password reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        refreshTokenService.deleteByUserId(user.getId());
        passwordResetTokenRepository.delete(resetToken);

        auditService.logEvent(user.getId(), AuditEventType.PASSWORD_RESET_COMPLETED);

        return new GenericResponse("Password successfully reset.");
    }

    @Transactional
    public AuthResponse refreshToken(String requestRefreshToken) {
        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token is not in database!"));

        User user = refreshToken.getUser();
        
        // Refresh token rotation: delete the old token and create a new one
        refreshTokenService.deleteToken(refreshToken);
        
        String jwtToken = jwtService.generateToken(user);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());
        
        auditService.logEvent(user.getId(), AuditEventType.TOKEN_REFRESH);
        
        return new AuthResponse(jwtToken, newRefreshToken.getToken());
    }
}
