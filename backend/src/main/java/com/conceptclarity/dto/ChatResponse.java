package com.conceptclarity.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatResponse(
        Long conversationId,
        Long queryId,
        Long explanationId,
        String reply,
        String level,
        String topic,
        int topicFrequency,
        LocalDateTime createdAt,
        List<String> recommendedTopics
) {
}
