package com.conceptclarity.service;

import com.conceptclarity.model.LearningProgress;
import com.conceptclarity.model.TopicTracking;
import com.conceptclarity.model.User;
import com.conceptclarity.repository.LearningProgressRepository;
import com.conceptclarity.repository.TopicTrackingRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationMemoryService {

    private final TopicTrackingRepository topicTrackingRepository;
    private final LearningProgressRepository learningProgressRepository;

    public ConversationMemoryService(TopicTrackingRepository topicTrackingRepository,
                                     LearningProgressRepository learningProgressRepository) {
        this.topicTrackingRepository = topicTrackingRepository;
        this.learningProgressRepository = learningProgressRepository;
    }

    @Transactional
    public int trackTopicFrequency(User user, String topic) {
        String normalizedTopic = normalizeTopic(topic);
        TopicTracking tracking = topicTrackingRepository.findByUserIdAndNormalizedTopic(user.getId(), normalizedTopic)
                .orElseGet(() -> TopicTracking.builder()
                        .user(user)
                        .topic(topic)
                        .normalizedTopic(normalizedTopic)
                        .frequency(0)
                        .currentLevel("Beginner")
                        .lastAskedAt(LocalDateTime.now())
                        .build());

        int nextFrequency = tracking.getFrequency() + 1;
        tracking.setTopic(topic);
        tracking.setFrequency(nextFrequency);
        tracking.setCurrentLevel(levelForFrequency(nextFrequency));
        tracking.setLastAskedAt(LocalDateTime.now());
        topicTrackingRepository.save(tracking);
        return nextFrequency;
    }

    @Transactional(readOnly = true)
    public String getExplanationLevel(User user, String topic) {
        int currentFrequency = topicTrackingRepository
                .findByUserIdAndNormalizedTopic(user.getId(), normalizeTopic(topic))
                .map(TopicTracking::getFrequency)
                .orElse(0);
        return levelForFrequency(currentFrequency + 1);
    }

    @Transactional
    public void updateUserLearningProgress(User user, String level) {
        LearningProgress progress = learningProgressRepository.findByUserId(user.getId())
                .orElseGet(() -> LearningProgress.builder()
                        .user(user)
                        .totalInteractions(0)
                        .beginnerCount(0)
                        .intermediateCount(0)
                        .advancedCount(0)
                        .expertCount(0)
                        .updatedAt(LocalDateTime.now())
                        .build());

        progress.setTotalInteractions(progress.getTotalInteractions() + 1);
        switch (level) {
            case "Expert" -> progress.setExpertCount(progress.getExpertCount() + 1);
            case "Advanced" -> progress.setAdvancedCount(progress.getAdvancedCount() + 1);
            case "Intermediate" -> progress.setIntermediateCount(progress.getIntermediateCount() + 1);
            default -> progress.setBeginnerCount(progress.getBeginnerCount() + 1);
        }
        progress.setUpdatedAt(LocalDateTime.now());
        learningProgressRepository.save(progress);
    }

    private String levelForFrequency(int frequency) {
        if (frequency <= 1) {
            return "Beginner";
        }
        if (frequency == 2) {
            return "Intermediate";
        }
        if (frequency == 3) {
            return "Advanced";
        }
        return "Expert";
    }

    private String normalizeTopic(String topic) {
        return topic == null ? "" : topic.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
