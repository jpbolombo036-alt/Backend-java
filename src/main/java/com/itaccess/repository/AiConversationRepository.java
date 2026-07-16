package com.itaccess.repository;

import com.itaccess.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    Page<AiConversation> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
}
