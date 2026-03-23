package com.campuspulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityPostRequest(
        @NotBlank(message = "Token is required")
        String token,
        @NotBlank(message = "Content is required")
        @Size(max = 1000, message = "Content must be under 1000 characters")
        String content
) {
}
