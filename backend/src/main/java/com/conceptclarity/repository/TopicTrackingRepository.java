package com.conceptclarity.repository;

import com.conceptclarity.model.TopicTracking;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicTrackingRepository extends JpaRepository<TopicTracking, Long> {
    Optional<TopicTracking> findByUserIdAndNormalizedTopic(Long userId, String normalizedTopic);
}
