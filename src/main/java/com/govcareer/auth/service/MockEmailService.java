package com.govcareer.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("local")
public class MockEmailService implements EmailService {

    @Async
    @Override
    public void sendVerificationEmail(String to, String token) {
        log.info("========================================");
        log.info("MOCK EMAIL SERVICE: Verification Email");
        log.info("To: {}", to);
        log.info("Subject: Verify your GovCareer Account");
        log.info("Token: {}", token);
        log.info("Link: http://localhost:8787/api/auth/verify-email?token={}", token);
        log.info("========================================");
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        log.info("========================================");
        log.info("MOCK EMAIL SERVICE: Password Reset Email");
        log.info("To: {}", to);
        log.info("Subject: GovCareer Password Reset Request");
        log.info("Token: {}", token);
        log.info("Link: http://localhost:8787/api/auth/reset-password?token={}", token);
        log.info("========================================");
    }
}
