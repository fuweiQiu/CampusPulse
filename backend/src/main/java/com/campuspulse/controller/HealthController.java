package com.campuspulse.controller;

import com.campuspulse.dto.ChatRequest;
import com.campuspulse.dto.ChatConversationResponse;
import com.campuspulse.dto.MetricsResponse;
import com.campuspulse.service.ChatConversationService;
import com.campuspulse.service.MetricsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final ChatConversationService chatConversationService;
    private final MetricsService metricsService;

    public HealthController(ChatConversationService chatConversationService, MetricsService metricsService) {
        this.chatConversationService = chatConversationService;
        this.metricsService = metricsService;
    }

    @PostMapping("/chat")
    public ChatConversationResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatConversationService.processMessage(request);
    }

    @GetMapping("/chat/history")
    public ChatConversationResponse chatHistory(@RequestParam String token) {
        return chatConversationService.history(token);
    }

    @GetMapping("/metrics")
    public MetricsResponse metrics(@RequestParam String token) {
        return metricsService.buildWeeklyMetrics(token);
    }
}
