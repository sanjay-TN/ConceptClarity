package com.conceptclarity.repository;

import com.conceptclarity.model.ConversationHistory;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationHistoryRepository extends JpaRepository<ConversationHistory, Long> {

    @Query("""
            select ch from ConversationHistory ch
            where ch.user.id = :userId
            order by ch.createdAt desc
            """)
    List<ConversationHistory> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
}
