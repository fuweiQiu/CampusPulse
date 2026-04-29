package com.campuspulse.dto;

import java.time.LocalDateTime;

public record ObservationSummaryResponse(
        Long recordId,
        String fhirId,
        String resourceUrl,
        String status,
        Integer stressScore,
        Double sleepHours,
        String emotionDisplay,
        String sourceText,
        String suggestion,
        LocalDateTime effectiveDateTime
) {
}
