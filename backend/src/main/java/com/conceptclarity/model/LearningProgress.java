package com.conceptclarity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "learning_progress")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private int totalInteractions;

    @Column(nullable = false)
    private int beginnerCount;

    @Column(nullable = false)
    private int intermediateCount;

    @Column(nullable = false)
    private int advancedCount;

    @Column(nullable = false)
    private int expertCount;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
