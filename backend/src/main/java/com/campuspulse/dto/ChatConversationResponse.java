package com.campuspulse.dto;

import java.util.List;

public record ChatConversationResponse(
        List<ChatMessageResponse> messages,
        boolean awaitingFollowUp,
        String pendingField,
        ObservationSummaryResponse latestObservation,
        Object latestFhirObservation
) {
}
