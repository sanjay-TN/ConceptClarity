package com.conceptclarity.dto;

import java.util.List;

public record ProgressResponse(
        long totalExplanations,
        long favoriteCount,
        long beginnerCount,
        long intermediateCount,
        long advancedCount,
        int learningScore,
        List<String> recentTopics,
        List<String> recommendedTopics
) {
}
