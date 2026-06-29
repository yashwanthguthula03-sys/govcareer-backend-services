package com.govcareer.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("!local")
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Async
    @Override
    public void sendVerificationEmail(String to, String token) {
        log.info("Sending Email Verification link to {}: /api/auth/verify-email?token={}", to, token);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Verify your GovCareer Account");
            message.setText("Please verify your account by using the following token: " + token + 
                    "\nOr visit: http://localhost:8787/api/auth/verify-email?token=" + token);
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send verification email to {}", to, e);
        }
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        log.info("Sending Password Reset link to {}: /api/auth/reset-password?token={}", to, token);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("GovCareer Password Reset Request");
            message.setText("Please reset your password using the following token: " + token + 
                    "\nOr visit: http://localhost:8787/api/auth/reset-password?token=" + token);
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send password reset email to {}", to, e);
        }
    }
}
