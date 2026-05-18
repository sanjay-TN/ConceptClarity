package com.conceptclarity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "topic_tracking", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "normalized_topic"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 240)
    private String topic;

    @Column(name = "normalized_topic", nullable = false, length = 240)
    private String normalizedTopic;

    @Column(nullable = false)
    private int frequency;

    @Column(nullable = false, length = 40)
    private String currentLevel;

    @Column(nullable = false)
    private LocalDateTime lastAskedAt;
}
