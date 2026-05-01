package com.campuspulse.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.campuspulse.dto.MetricsResponse;
import com.campuspulse.model.ObservationRecord;
import com.campuspulse.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetricsService {

    private final AuthService authService;
    private final FhirResourceService fhirResourceService;

    public MetricsService(AuthService authService, FhirResourceService fhirResourceService) {
        this.authService = authService;
        this.fhirResourceService = fhirResourceService;
    }

    @Transactional(readOnly = true)
    public MetricsResponse buildWeeklyMetrics(String token) {
        User user = authService.authenticate(token);
        LocalDate startDate = LocalDate.now().minusDays(6);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        List<ObservationRecord> observations = fhirResourceService.preferredObservationRecords(user, start, end);

        List<ObservationRecord> stressObservations = observations.stream()
                .filter(observation -> observation.getStressScore() != null)
                .toList();
        List<ObservationRecord> sleepObservations = observations.stream()
                .filter(observation -> observation.getSleepHours() != null)
                .toList();
        List<ObservationRecord> emotionObservations = observations.stream()
                .filter(observation -> observation.getEmotionDisplay() != null)
                .toList();

        double averageStress = round(stressObservations.stream()
                .mapToInt(ObservationRecord::getStressScore)
                .average()
                .orElse(0.0));
        double averageSleep = round(sleepObservations.stream()
                .mapToDouble(ObservationRecord::getSleepHours)
                .average()
                .orElse(0.0));

        Map<String, Long> emotionDistribution = emotionObservations.stream()
                .collect(Collectors.groupingBy(ObservationRecord::getEmotionDisplay, LinkedHashMap::new, Collectors.counting()));

        Map<LocalDate, List<ObservationRecord>> byDate = observations.stream()
                .collect(Collectors.groupingBy(observation -> observation.getEffectiveDateTime().toLocalDate()));

        List<MetricsResponse.DailyMetric> dailyMetrics = new ArrayList<>();
        for (int offset = 0; offset < 7; offset++) {
            LocalDate date = startDate.plusDays(offset);
            List<ObservationRecord> dailyObservations = byDate.getOrDefault(date, List.of());
            double dailyStress = dailyObservations.stream()
                    .filter(observation -> observation.getStressScore() != null)
                    .mapToInt(ObservationRecord::getStressScore)
                    .average()
                    .orElse(0.0);
            double dailySleep = dailyObservations.stream()
                    .filter(observation -> observation.getSleepHours() != null)
                    .mapToDouble(ObservationRecord::getSleepHours)
                    .average()
                    .orElse(0.0);
            dailyMetrics.add(new MetricsResponse.DailyMetric(
                    date.toString(),
                    round(dailyStress),
                    round(dailySleep)
            ));
        }

        return new MetricsResponse(averageStress, averageSleep, emotionDistribution, dailyMetrics, observations.size());
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
