package com.campuspulse.service;

import java.util.Comparator;
import java.util.List;

import com.campuspulse.dto.ChatConversationResponse;
import com.campuspulse.dto.ChatMessageResponse;
import com.campuspulse.dto.ChatRequest;
import com.campuspulse.dto.ObservationSummaryResponse;
import com.campuspulse.model.ChatMessage;
import com.campuspulse.model.ChatSender;
import com.campuspulse.model.ChatSession;
import com.campuspulse.model.ChatSessionStatus;
import com.campuspulse.model.ObservationRecord;
import com.campuspulse.model.PendingField;
import com.campuspulse.model.User;
import com.campuspulse.repository.ChatMessageRepository;
import com.campuspulse.repository.ChatSessionRepository;
import com.campuspulse.repository.ObservationRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatConversationService {

    private static final String UNKNOWN = "unknown";

    private final AuthService authService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObservationRecordRepository observationRecordRepository;
    private final ChatInsightExtractor chatInsightExtractor;
    private final FhirResourceService fhirResourceService;

    public ChatConversationService(
            AuthService authService,
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            ObservationRecordRepository observationRecordRepository,
            ChatInsightExtractor chatInsightExtractor,
            FhirResourceService fhirResourceService
    ) {
        this.authService = authService;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.observationRecordRepository = observationRecordRepository;
        this.chatInsightExtractor = chatInsightExtractor;
        this.fhirResourceService = fhirResourceService;
    }

    @Transactional
    public ChatConversationResponse processMessage(ChatRequest request) {
        User user = authService.authenticate(request.token());
        ChatSession session = chatSessionRepository.findFirstByUserAndStatusOrderByUpdatedAtDesc(user, ChatSessionStatus.COLLECTING)
                .orElseGet(() -> createSession(user));

        saveMessage(user, session, ChatSender.USER, request.message().trim());
        mergeNarrative(session, request.message().trim());

        ChatInsightExtractor.ExtractedInsights insights = chatInsightExtractor.extract(request.message());
        mergeInsights(session, insights);
        session.setPendingField(nextPendingField(session));
        chatSessionRepository.save(session);

        if (session.getPendingField() != null) {
            String prompt = buildPrompt(session.getPendingField());
            saveMessage(user, session, ChatSender.BOT, prompt);
            return buildResponse(user, session, null);
        }

        if (!hasMeaningfulData(session)) {
            String prompt = "謝謝你願意先說這些。我這次先不急著幫你建立紀錄，因為還缺少足夠的可量化資訊；下次只要再補一句壓力分數、睡眠時數，或你當下的情緒感受其中一項，我就能接著幫你整理。";
            session.setStatus(ChatSessionStatus.COMPLETED);
            session.setPendingField(null);
            chatSessionRepository.save(session);
            saveMessage(user, session, ChatSender.BOT, prompt);
            return buildResponse(user, null, null);
        }

        String suggestion = buildSuggestion(session);
        ObservationRecord record = fhirResourceService.createObservationRecord(user, session, suggestion);
        session.setStatus(ChatSessionStatus.COMPLETED);
        session.setPendingField(null);
        chatSessionRepository.save(session);

        saveMessage(user, session, ChatSender.BOT, buildCompletionMessage(record));
        return buildResponse(user, null, record);
    }

    @Transactional(readOnly = true)
    public ChatConversationResponse history(String token) {
        User user = authService.authenticate(token);
        ChatSession activeSession = chatSessionRepository.findFirstByUserAndStatusOrderByUpdatedAtDesc(user, ChatSessionStatus.COLLECTING).orElse(null);
        ObservationRecord latestRecord = observationRecordRepository.findTopByUserOrderByEffectiveDateTimeDesc(user).orElse(null);
        return buildResponse(user, activeSession, latestRecord);
    }

    private ChatSession createSession(User user) {
        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setStatus(ChatSessionStatus.COLLECTING);
        return chatSessionRepository.save(session);
    }

    private void mergeNarrative(ChatSession session, String incomingMessage) {
        String existing = session.getSourceText();
        session.setSourceText(existing == null || existing.isBlank()
                ? incomingMessage
                : existing + "\n" + incomingMessage);
    }

    private void mergeInsights(ChatSession session, ChatInsightExtractor.ExtractedInsights insights) {
        if (insights.stressScore() != null) {
            session.setStressScore(insights.stressScore());
            session.setStressAbsentReasonCode(null);
        } else if (session.getPendingField() == PendingField.STRESS_SCORE && insights.unknownReply()) {
            session.setStressAbsentReasonCode(UNKNOWN);
        }

        if (insights.sleepHours() != null) {
            session.setSleepHours(insights.sleepHours());
            session.setSleepAbsentReasonCode(null);
        } else if (session.getPendingField() == PendingField.SLEEP_HOURS && insights.unknownReply()) {
            session.setSleepAbsentReasonCode(UNKNOWN);
        }

        if (insights.emotionCode() != null) {
            session.setEmotionCode(insights.emotionCode());
            session.setEmotionDisplay(insights.emotionDisplay());
            session.setEmotionAbsentReasonCode(null);
        } else if (session.getPendingField() == PendingField.EMOTION && insights.unknownReply()) {
            session.setEmotionAbsentReasonCode(UNKNOWN);
        }
    }

    private PendingField nextPendingField(ChatSession session) {
        if (session.getStressScore() == null && session.getStressAbsentReasonCode() == null) {
            return PendingField.STRESS_SCORE;
        }
        if (session.getSleepHours() == null && session.getSleepAbsentReasonCode() == null) {
            return PendingField.SLEEP_HOURS;
        }
        if (session.getEmotionCode() == null && session.getEmotionAbsentReasonCode() == null) {
            return PendingField.EMOTION;
        }
        return null;
    }

    private boolean hasMeaningfulData(ChatSession session) {
        return session.getStressScore() != null
                || session.getSleepHours() != null
                || session.getEmotionCode() != null;
    }

    private String buildPrompt(PendingField field) {
        return switch (field) {
            case STRESS_SCORE ->
                    "我收到你剛剛的狀態了。想先幫你把這筆紀錄補完整一點，如果把現在的壓力放在 0 到 10 分，大概會是幾分？如果一時說不上來，直接回我「不知道」也可以。";
            case SLEEP_HOURS ->
                    "謝謝你先告訴我這些。我還想再確認一個很重要的身體訊號：你最近一晚大約睡了幾小時？如果真的不確定，也可以直接說不知道，我就先留空不亂填。";
            case EMOTION ->
                    "我大概理解你的狀態了，再陪你補最後一格。你現在比較接近平靜、焦慮、疲憊、壓力大，還是別的感受？如果你不想回答，也可以直接說不知道。";
        };
    }

    private String buildSuggestion(ChatSession session) {
        if (session.getStressScore() != null && session.getStressScore() >= 8) {
            return "你現在的壓力明顯偏高，先不要急著把所有事一次扛完。把待辦拆小、暫時離開螢幕幾分鐘，通常會比硬撐更有幫助；如果這種高壓連續幾天都在，值得考慮找校園諮商或信任的人聊聊。";
        }
        if (session.getSleepHours() != null && session.getSleepHours() < 5.5) {
            return "你這段時間的睡眠有點不夠，身體其實很容易因此放大壓力感。今晚如果做得到，先把睡前的刺激降下來，讓自己早一點休息，通常會比硬撐更有效。";
        }
        if ("Overwhelmed".equals(session.getEmotionDisplay()) || "Exhausted".equals(session.getEmotionDisplay())) {
            return "我看到你現在的負荷有點重，先讓自己慢下來是合理的。先喝點水、動一動身體、把今天非必要的事往後放，若願意的話，也找一個讓你安心的人說說現在的感受。";
        }
        return "我先幫你把這次狀態整理好了。之後如果願意持續記錄，會更容易看見壓力、睡眠和情緒之間是怎麼互相影響的。";
    }

    private String buildCompletionMessage(ObservationRecord record) {
        String sleepText = record.getSleepHours() == null ? "未提供或標記 unknown" : record.getSleepHours() + " 小時";
        String stressText = record.getStressScore() == null ? "未提供或標記 unknown" : record.getStressScore() + "/10";
        String emotionText = record.getEmotionDisplay() == null ? "未提供或標記 unknown" : record.getEmotionDisplay();

        return "我已經把這次對話整理成一筆 FHIR Observation。這次的壓力是 " + stressText
                + "，睡眠是 " + sleepText
                + "，情緒標籤是 " + emotionText
                + "。如果你想把這次紀錄留存下來，之後也能到儀表板下載 FHIR Bundle。";
    }

    private void saveMessage(User user, ChatSession session, ChatSender sender, String content) {
        ChatMessage message = new ChatMessage();
        message.setUser(user);
        message.setSession(session);
        message.setSender(sender);
        message.setContent(content);
        chatMessageRepository.save(message);
    }

    private ChatConversationResponse buildResponse(User user, ChatSession session, ObservationRecord latestRecord) {
        List<ChatMessageResponse> messages = chatMessageRepository.findTop60ByUserOrderByCreatedAtDesc(user)
                .stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                .map(message -> new ChatMessageResponse(
                        message.getId(),
                        message.getSender().name().toLowerCase(),
                        message.getContent(),
                        message.getCreatedAt()
                ))
                .toList();

        ObservationRecord record = latestRecord != null
                ? latestRecord
                : observationRecordRepository.findTopByUserOrderByEffectiveDateTimeDesc(user).orElse(null);

        ObservationSummaryResponse summary = record == null
                ? null
                : new ObservationSummaryResponse(
                record.getId(),
                record.getFhirId(),
                record.getResourceUrl(),
                record.getStatus(),
                record.getStressScore(),
                record.getSleepHours(),
                record.getEmotionDisplay(),
                record.getSourceText(),
                record.getSuggestion(),
                record.getEffectiveDateTime()
        );

        Object latestFhir = record == null ? null : fhirResourceService.readJson(record.getResourceJson());

        return new ChatConversationResponse(
                messages,
                session != null && session.getPendingField() != null,
                session == null || session.getPendingField() == null ? null : session.getPendingField().name(),
                summary,
                latestFhir
        );
    }
}
