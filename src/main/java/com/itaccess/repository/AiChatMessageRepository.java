package com.itaccess.repository;

import com.itaccess.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {
    Page<AiChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);
}
