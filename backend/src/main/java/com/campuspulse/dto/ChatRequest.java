package com.campuspulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Token is required")
        String token,
        @NotBlank(message = "Message is required")
        @Size(max = 500, message = "Message must be under 500 characters")
        String message
) {
}
