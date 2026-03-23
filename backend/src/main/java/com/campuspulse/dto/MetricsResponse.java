package com.campuspulse.dto;

import java.util.List;
import java.util.Map;

public record MetricsResponse(
        Double averageStressScore,
        Double averageSleepHours,
        Map<String, Long> emotionDistribution,
        List<DailyMetric> dailyMetrics,
        long totalCheckIns
) {
    public record DailyMetric(
            String date,
            Double averageStressScore,
            Double averageSleepHours
    ) {
    }
}
