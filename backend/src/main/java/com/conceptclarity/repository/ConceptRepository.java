package com.conceptclarity.repository;

import com.conceptclarity.model.ConceptQuery;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConceptRepository extends JpaRepository<ConceptQuery, Long> {

    @Query("""
            select cq from ConceptQuery cq
            left join fetch cq.explanation e
            where cq.user.id = :userId
            order by cq.createdAt desc
            """)
    List<ConceptQuery> findHistoryByUserId(@Param("userId") Long userId);

    @Query("""
            select cq from ConceptQuery cq
            left join fetch cq.explanation e
            where cq.user.id = :userId
            and lower(cq.topic) like lower(concat('%', :keyword, '%'))
            order by cq.createdAt desc
            """)
    List<ConceptQuery> searchHistory(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Query(value = """
            select cq from ConceptQuery cq
            left join fetch cq.explanation e
            where cq.user.id = :userId
            and (:keyword is null or lower(cq.topic) like lower(concat('%', :keyword, '%')))
            and (:favoriteOnly = false or e.favorite = true)
            order by cq.createdAt desc
            """,
            countQuery = """
            select count(cq) from ConceptQuery cq
            join cq.explanation e
            where cq.user.id = :userId
            and (:keyword is null or lower(cq.topic) like lower(concat('%', :keyword, '%')))
            and (:favoriteOnly = false or e.favorite = true)
            """)
    Page<ConceptQuery> findHistoryPage(@Param("userId") Long userId,
                                       @Param("keyword") String keyword,
                                       @Param("favoriteOnly") boolean favoriteOnly,
                                       Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndLevel(Long userId, String level);
}
