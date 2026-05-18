package com.conceptclarity.service;

import com.conceptclarity.ai.AIExplanationService;
import com.conceptclarity.ai.LocalAIEngine;
import com.conceptclarity.dto.ExplanationRequest;
import com.conceptclarity.dto.ExplanationResponse;
import com.conceptclarity.exception.BadRequestException;
import com.conceptclarity.model.ConceptQuery;
import com.conceptclarity.model.Explanation;
import com.conceptclarity.model.SearchHistory;
import com.conceptclarity.model.User;
import com.conceptclarity.repository.ConceptRepository;
import com.conceptclarity.repository.SearchHistoryRepository;
import com.conceptclarity.util.InputSanitizer;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExplanationService {

    private final AIExplanationService aiExplanationService;
    private final LocalAIEngine localAIEngine;
    private final ConceptRepository conceptRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final UserService userService;
    private final InputSanitizer inputSanitizer;

    public ExplanationService(AIExplanationService aiExplanationService,
                              LocalAIEngine localAIEngine,
                              ConceptRepository conceptRepository,
                              SearchHistoryRepository searchHistoryRepository,
                              UserService userService,
                              InputSanitizer inputSanitizer) {
        this.aiExplanationService = aiExplanationService;
        this.localAIEngine = localAIEngine;
        this.conceptRepository = conceptRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.userService = userService;
        this.inputSanitizer = inputSanitizer;
    }

    @Transactional
    public ExplanationResponse explain(ExplanationRequest request) {
        String level = canonicalLevel(request.level());
        String explanationType = canonicalType(request.explanationType());
        User user = userService.getUser(request.userId());
        String topic = inputSanitizer.cleanText(request.topic(), 240);
        if (topic.length() < 2) {
            throw new BadRequestException("Topic must contain at least 2 characters.");
        }
        String content = aiExplanationService.generateByLevel(topic, level, explanationType);

        ConceptQuery query = ConceptQuery.builder()
                .user(user)
                .topic(topic)
                .level(level)
                .explanationType(explanationType)
                .createdAt(LocalDateTime.now())
                .build();

        Explanation explanation = Explanation.builder()
                .conceptQuery(query)
                .content(content)
                .favorite(false)
                .createdAt(LocalDateTime.now())
                .build();

        query.setExplanation(explanation);
        ConceptQuery saved = conceptRepository.save(query);

        searchHistoryRepository.save(SearchHistory.builder()
                .user(user)
                .topic(topic)
                .detectedDomain(localAIEngine.topicClassifier(topic))
                .createdAt(LocalDateTime.now())
                .build());

        return toResponse(saved);
    }

    public ExplanationResponse toResponse(ConceptQuery query) {
        Explanation explanation = query.getExplanation();
        return new ExplanationResponse(
                query.getId(),
                explanation.getId(),
                query.getTopic(),
                query.getLevel(),
                query.getExplanationType(),
                explanation.getContent(),
                explanation.isFavorite(),
                query.getCreatedAt(),
                localAIEngine.topicClassifier(query.getTopic()),
                localAIEngine.detectConceptComplexity(query.getTopic()),
                localAIEngine.keywordAnalyzer(query.getTopic()),
                localAIEngine.recommendedTopics(query.getTopic())
        );
    }

    private String canonicalLevel(String level) {
        return switch (normalize(level)) {
            case "beginner" -> "Beginner";
            case "intermediate" -> "Intermediate";
            case "advanced" -> "Advanced";
            default -> throw new BadRequestException("Explanation level must be Beginner, Intermediate, or Advanced.");
        };
    }

    private String canonicalType(String explanationType) {
        return switch (normalize(explanationType)) {
            case "definition", "define" -> "Definition";
            case "detailed explanation", "detailed", "detail", "explanation" -> "Detailed Explanation";
            case "step-by-step", "step by step", "stepwise", "steps" -> "Step-by-Step";
            default -> throw new BadRequestException("Explanation type must be Definition, Detailed Explanation, or Step-by-Step.");
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
