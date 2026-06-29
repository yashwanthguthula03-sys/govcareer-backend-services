package com.govcareer.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}
