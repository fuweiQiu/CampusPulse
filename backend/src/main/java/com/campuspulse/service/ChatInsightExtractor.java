package com.campuspulse.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class ChatInsightExtractor {

    private static final Pattern SLEEP_HOURS_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d{1,2}(?:\\.\\d+)?)\\s*(?:小時|hr|hrs|hours|h)(?![a-z])",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern STRESS_PREFIX_PATTERN = Pattern.compile(
            "(?:壓力|stress)[^0-9]{0,10}(10|[0-9])",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern STRESS_SUFFIX_PATTERN = Pattern.compile(
            "(10|[0-9])\\s*(?:分|/10)[^\\p{IsHan}a-zA-Z0-9]{0,3}(?:壓力|stress)?",
            Pattern.CASE_INSENSITIVE
    );
    private static final String UNKNOWN = "unknown";

    private static final Map<String, Integer> STRESS_KEYWORDS = new LinkedHashMap<>();
    private static final Map<String, EmotionSignal> EMOTION_KEYWORDS = new LinkedHashMap<>();

    static {
        STRESS_KEYWORDS.put("壓力大", 8);
        STRESS_KEYWORDS.put("焦慮", 8);
        STRESS_KEYWORDS.put("喘不過氣", 9);
        STRESS_KEYWORDS.put("崩潰", 9);
        STRESS_KEYWORDS.put("頭痛", 7);
        STRESS_KEYWORDS.put("deadline", 7);
        STRESS_KEYWORDS.put("考試", 7);
        STRESS_KEYWORDS.put("報告", 6);
        STRESS_KEYWORDS.put("緊張", 6);
        STRESS_KEYWORDS.put("煩", 6);
        STRESS_KEYWORDS.put("平靜", 2);
        STRESS_KEYWORDS.put("放鬆", 2);
        STRESS_KEYWORDS.put("穩定", 3);
        STRESS_KEYWORDS.put("心情不錯", 2);

        EMOTION_KEYWORDS.put("開心", new EmotionSignal("positive", "Positive"));
        EMOTION_KEYWORDS.put("放鬆", new EmotionSignal("steady", "Steady"));
        EMOTION_KEYWORDS.put("平靜", new EmotionSignal("steady", "Steady"));
        EMOTION_KEYWORDS.put("焦慮", new EmotionSignal("anxious", "Anxious"));
        EMOTION_KEYWORDS.put("壓力大", new EmotionSignal("stressed", "Stressed"));
        EMOTION_KEYWORDS.put("緊張", new EmotionSignal("stressed", "Stressed"));
        EMOTION_KEYWORDS.put("頭痛", new EmotionSignal("exhausted", "Exhausted"));
        EMOTION_KEYWORDS.put("疲倦", new EmotionSignal("exhausted", "Exhausted"));
        EMOTION_KEYWORDS.put("好累", new EmotionSignal("exhausted", "Exhausted"));
        EMOTION_KEYWORDS.put("崩潰", new EmotionSignal("overwhelmed", "Overwhelmed"));
        EMOTION_KEYWORDS.put("喘不過氣", new EmotionSignal("overwhelmed", "Overwhelmed"));
    }

    public ExtractedInsights extract(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        Integer stressScore = extractStress(normalized);
        Double sleepHours = extractSleepHours(normalized);
        EmotionSignal emotionSignal = extractEmotion(normalized);
        boolean unknown = containsUnknown(normalized);

        return new ExtractedInsights(
                stressScore,
                sleepHours,
                emotionSignal == null ? null : emotionSignal.code(),
                emotionSignal == null ? null : emotionSignal.display(),
                unknown
        );
    }

    private Integer extractStress(String normalized) {
        Matcher prefix = STRESS_PREFIX_PATTERN.matcher(normalized);
        if (prefix.find()) {
            return Integer.parseInt(prefix.group(1));
        }

        Matcher suffix = STRESS_SUFFIX_PATTERN.matcher(normalized);
        if (suffix.find() && (normalized.contains("壓力") || normalized.contains("stress"))) {
            return Integer.parseInt(suffix.group(1));
        }

        Integer inferred = null;
        for (Map.Entry<String, Integer> entry : STRESS_KEYWORDS.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                inferred = entry.getValue();
            }
        }
        return inferred;
    }

    private Double extractSleepHours(String normalized) {
        if (normalized.contains("沒睡") || normalized.contains("通宵")) {
            return 0.0;
        }

        Matcher matcher = SLEEP_HOURS_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }

        double parsed = Double.parseDouble(matcher.group(1));
        return Math.round(Math.max(0.0, Math.min(24.0, parsed)) * 10.0) / 10.0;
    }

    private EmotionSignal extractEmotion(String normalized) {
        for (Map.Entry<String, EmotionSignal> entry : EMOTION_KEYWORDS.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean containsUnknown(String normalized) {
        return normalized.contains("不知道")
                || normalized.contains("不確定")
                || normalized.contains("忘了")
                || normalized.contains("沒注意")
                || normalized.contains("略過")
                || normalized.contains("skip");
    }

    public record ExtractedInsights(
            Integer stressScore,
            Double sleepHours,
            String emotionCode,
            String emotionDisplay,
            boolean unknownReply
    ) {
    }

    private record EmotionSignal(String code, String display) {
    }
}
