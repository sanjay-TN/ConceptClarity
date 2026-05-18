package com.conceptclarity.service;

import com.conceptclarity.ai.LocalAIEngine;
import com.conceptclarity.dto.ApiMessageResponse;
import com.conceptclarity.dto.ExplanationResponse;
import com.conceptclarity.dto.PageResponse;
import com.conceptclarity.dto.ProgressResponse;
import com.conceptclarity.dto.SuggestionResponse;
import com.conceptclarity.exception.ResourceNotFoundException;
import com.conceptclarity.model.Explanation;
import com.conceptclarity.model.Favorite;
import com.conceptclarity.model.User;
import com.conceptclarity.repository.ConceptRepository;
import com.conceptclarity.repository.ExplanationRepository;
import com.conceptclarity.repository.FavoriteRepository;
import com.conceptclarity.repository.SearchHistoryRepository;
import com.conceptclarity.util.InputSanitizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryService {

    private final ConceptRepository conceptRepository;
    private final ExplanationRepository explanationRepository;
    private final FavoriteRepository favoriteRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final UserService userService;
    private final ExplanationService explanationService;
    private final LocalAIEngine localAIEngine;
    private final InputSanitizer inputSanitizer;

    public HistoryService(ConceptRepository conceptRepository,
                          ExplanationRepository explanationRepository,
                          FavoriteRepository favoriteRepository,
                          SearchHistoryRepository searchHistoryRepository,
                          UserService userService,
                          ExplanationService explanationService,
                          LocalAIEngine localAIEngine,
                          InputSanitizer inputSanitizer) {
        this.conceptRepository = conceptRepository;
        this.explanationRepository = explanationRepository;
        this.favoriteRepository = favoriteRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.userService = userService;
        this.explanationService = explanationService;
        this.localAIEngine = localAIEngine;
        this.inputSanitizer = inputSanitizer;
    }

    @Transactional(readOnly = true)
    public List<ExplanationResponse> history(Long userId, String search) {
        userService.getUser(userId);
        String keyword = inputSanitizer.cleanText(search, 120);
        return (keyword.isBlank()
                ? conceptRepository.findHistoryByUserId(userId)
                : conceptRepository.searchHistory(userId, keyword))
                .stream()
                .map(explanationService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ExplanationResponse> historyPage(Long userId,
                                                         String search,
                                                         boolean favoriteOnly,
                                                         int page,
                                                         int size) {
        userService.getUser(userId);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 24));
        String keyword = inputSanitizer.cleanText(search, 120);
        Page<ExplanationResponse> result = conceptRepository.findHistoryPage(
                        userId,
                        keyword.isBlank() ? null : keyword,
                        favoriteOnly,
                        PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .map(explanationService::toResponse);
        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }

    @Transactional
    public ApiMessageResponse deleteHistory(Long userId, Long queryId) {
        var query = conceptRepository.findById(queryId)
                .filter(item -> item.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("History item was not found."));
        conceptRepository.delete(query);
        return new ApiMessageResponse("History item deleted.");
    }

    @Transactional
    public ExplanationResponse toggleFavorite(Long userId, Long explanationId) {
        User user = userService.getUser(userId);
        Explanation explanation = explanationRepository.findByIdAndConceptQueryUserId(explanationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Explanation was not found."));

        favoriteRepository.findByUserIdAndExplanationId(userId, explanationId)
                .ifPresentOrElse(favorite -> {
                    favoriteRepository.delete(favorite);
                    explanation.setFavorite(false);
                }, () -> {
                    Favorite favorite = Favorite.builder()
                            .user(user)
                            .explanation(explanation)
                            .createdAt(LocalDateTime.now())
                            .build();
                    favoriteRepository.save(favorite);
                    explanation.setFavorite(true);
                });

        return explanationService.toResponse(explanation.getConceptQuery());
    }

    @Transactional(readOnly = true)
    public SuggestionResponse suggestions(Long userId, String topic) {
        if (userId != null) {
            userService.getUser(userId);
        }
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        String cleanTopic = inputSanitizer.cleanText(topic, 120);
        if (!cleanTopic.isBlank()) {
            suggestions.addAll(localAIEngine.recommendedTopics(cleanTopic));
        }
        if (userId != null) {
            suggestions.addAll(searchHistoryRepository.findRecentTopics(userId, PageRequest.of(0, 6)));
        }
        suggestions.addAll(List.of(
                "Recursion",
                "DBMS Normalization",
                "REST APIs",
                "OOP",
                "Machine Learning",
                "Spring Boot Dependency Injection"
        ));
        return new SuggestionResponse(suggestions.stream().limit(10).toList());
    }

    @Transactional(readOnly = true)
    public ProgressResponse progress(Long userId) {
        userService.getUser(userId);
        long total = conceptRepository.countByUserId(userId);
        long favoriteCount = favoriteRepository.countByUserId(userId);
        long beginner = conceptRepository.countByUserIdAndLevel(userId, "Beginner");
        long intermediate = conceptRepository.countByUserIdAndLevel(userId, "Intermediate");
        long advanced = conceptRepository.countByUserIdAndLevel(userId, "Advanced");
        List<String> recent = searchHistoryRepository.findRecentTopics(userId, PageRequest.of(0, 6));
        List<String> recommended = new ArrayList<>();
        if (!recent.isEmpty()) {
            recommended.addAll(localAIEngine.recommendedTopics(recent.get(0)));
        }
        recommended.addAll(List.of("System Design", "Indexes", "Java Collections", "Cybersecurity Basics"));
        int score = (int) Math.min(100, total * 8 + favoriteCount * 5 + advanced * 6 + intermediate * 3);
        return new ProgressResponse(
                total,
                favoriteCount,
                beginner,
                intermediate,
                advanced,
                score,
                recent,
                recommended.stream().distinct().limit(6).toList()
        );
    }
}
