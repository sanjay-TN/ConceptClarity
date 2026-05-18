package com.conceptclarity.ai;

import org.springframework.stereotype.Service;

@Service
public class AIExplanationService {

    private final LocalAIEngine localAIEngine;

    public AIExplanationService(LocalAIEngine localAIEngine) {
        this.localAIEngine = localAIEngine;
    }

    public String generateByLevel(String topic, String level, String explanationType) {
        return localAIEngine.generate(topic, level, explanationType);
    }

    public String generateDefinition(String topic, String level) {
        return localAIEngine.generateDefinition(topic, level);
    }

    public String generateDetailedExplanation(String topic, String level) {
        return localAIEngine.generateDetailedExplanation(topic, level);
    }

    public String generateStepByStep(String topic, String level) {
        return localAIEngine.generateStepByStep(topic, level);
    }
}
