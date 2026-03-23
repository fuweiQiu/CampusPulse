package com.campuspulse.dto;

import java.time.LocalDateTime;

public record CommunityPostResponse(
        Long id,
        String content,
        LocalDateTime createdAt
) {
}
