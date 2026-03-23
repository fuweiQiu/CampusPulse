package com.campuspulse.dto;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        String sender,
        String content,
        LocalDateTime timestamp
) {
}
