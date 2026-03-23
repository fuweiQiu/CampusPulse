package com.campuspulse.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 32, message = "Username must be 3-32 characters")
        String username,
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 64, message = "Password must be 6-64 characters")
        String password,
        @Size(max = 80, message = "Display name must be under 80 characters")
        String displayName,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birthDate,
        @Pattern(
                regexp = "^(male|female|other|unknown)?$",
                message = "Gender must be male, female, other, or unknown"
        )
        String gender
) {
}
