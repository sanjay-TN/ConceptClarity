package com.conceptclarity.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ExplanationResponse(
        Long queryId,
        Long explanationId,
        String topic,
        String level,
        String explanationType,
        String content,
        boolean favorite,
        LocalDateTime createdAt,
        String detectedDomain,
        String complexity,
        List<String> keywords,
        List<String> recommendedTopics
) {
}
