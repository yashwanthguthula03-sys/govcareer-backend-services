package com.govcareer.auth.dto;

import com.govcareer.auth.entity.Role;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Role role
) {
}
