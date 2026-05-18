package com.conceptclarity.service;

import com.conceptclarity.ai.LocalAIEngine;
import com.conceptclarity.dto.ChatRequest;
import com.conceptclarity.dto.ChatResponse;
import com.conceptclarity.exception.BadRequestException;
import com.conceptclarity.model.ConceptQuery;
import com.conceptclarity.model.ConversationHistory;
import com.conceptclarity.model.Explanation;
import com.conceptclarity.model.SearchHistory;
import com.conceptclarity.model.User;
import com.conceptclarity.repository.ConceptRepository;
import com.conceptclarity.repository.ConversationHistoryRepository;
import com.conceptclarity.repository.SearchHistoryRepository;
import com.conceptclarity.util.InputSanitizer;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final LocalAIEngine localAIEngine;
    private final ConversationMemoryService conversationMemoryService;
    private final ConversationHistoryRepository conversationHistoryRepository;
    private final ConceptRepository conceptRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final UserService userService;
    private final InputSanitizer inputSanitizer;

    public ChatService(LocalAIEngine localAIEngine,
                       ConversationMemoryService conversationMemoryService,
                       ConversationHistoryRepository conversationHistoryRepository,
                       ConceptRepository conceptRepository,
                       SearchHistoryRepository searchHistoryRepository,
                       UserService userService,
                       InputSanitizer inputSanitizer) {
        this.localAIEngine = localAIEngine;
        this.conversationMemoryService = conversationMemoryService;
        this.conversationHistoryRepository = conversationHistoryRepository;
        this.conceptRepository = conceptRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.userService = userService;
        this.inputSanitizer = inputSanitizer;
    }

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        if (request.userId() == null) {
            throw new BadRequestException("User session is required for chat memory.");
        }
        User user = userService.getUser(request.userId());
        String message = inputSanitizer.cleanText(request.message(), 1000);
        if (message.length() < 2) {
            throw new BadRequestException("Message must contain at least 2 characters.");
        }

        String topic = localAIEngine.extractTopic(message);
        String level = conversationMemoryService.getExplanationLevel(user, topic);
        int topicFrequency = conversationMemoryService.trackTopicFrequency(user, topic);
        String reply = localAIEngine.generateChatResponse(message, topic, level, topicFrequency);
        LocalDateTime now = LocalDateTime.now();

        ConversationHistory conversation = conversationHistoryRepository.save(ConversationHistory.builder()
                .user(user)
                .message(message)
                .topic(topic)
                .level(level)
                .topicFrequency(topicFrequency)
                .reply(reply)
                .createdAt(now)
                .build());

        ConceptQuery query = ConceptQuery.builder()
                .user(user)
                .topic(topic)
                .level(level)
                .explanationType("Auto")
                .createdAt(now)
                .build();

        Explanation explanation = Explanation.builder()
                .conceptQuery(query)
                .content(reply)
                .favorite(false)
                .createdAt(now)
                .build();

        query.setExplanation(explanation);
        ConceptQuery savedQuery = conceptRepository.save(query);

        searchHistoryRepository.save(SearchHistory.builder()
                .user(user)
                .topic(topic)
                .detectedDomain(localAIEngine.topicClassifier(topic))
                .createdAt(now)
                .build());

        conversationMemoryService.updateUserLearningProgress(user, level);

        return new ChatResponse(
                conversation.getId(),
                savedQuery.getId(),
                savedQuery.getExplanation().getId(),
                reply,
                level,
                topic,
                topicFrequency,
                now,
                localAIEngine.recommendedTopics(topic)
        );
    }
}
