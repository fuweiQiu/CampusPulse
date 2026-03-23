package com.campuspulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 32, message = "Username must be 3-32 characters")
        String username,
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 64, message = "Password must be 6-64 characters")
        String password
) {
}
