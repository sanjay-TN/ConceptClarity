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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conversation_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, length = 240)
    private String topic;

    @Column(nullable = false, length = 40)
    private String level;

    @Column(nullable = false)
    private int topicFrequency;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reply;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
