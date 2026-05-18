package com.conceptclarity.repository;

import com.conceptclarity.model.KnowledgeBaseEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBaseEntry, Long> {
}
