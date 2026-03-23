package com.campuspulse.dto;

public record AuthResponse(
        String username,
        String displayName,
        String token,
        String patientFhirId,
        String message
) {
}
