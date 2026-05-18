package com.conceptclarity.repository;

import com.conceptclarity.model.Favorite;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserIdAndExplanationId(Long userId, Long explanationId);

    long countByUserId(Long userId);
}
