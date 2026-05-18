package com.conceptclarity.repository;

import com.conceptclarity.model.Explanation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExplanationRepository extends JpaRepository<Explanation, Long> {
    Optional<Explanation> findByIdAndConceptQueryUserId(Long id, Long userId);

    long countByConceptQueryUserIdAndFavoriteTrue(Long userId);
}
