package com.conceptclarity.repository;

import com.conceptclarity.model.SearchHistory;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    @Query("""
            select sh.topic from SearchHistory sh
            where sh.user.id = :userId
            order by sh.createdAt desc
            """)
    List<String> findRecentTopics(@Param("userId") Long userId, Pageable pageable);
}
