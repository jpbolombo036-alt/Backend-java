package com.itaccess.repository;

import com.itaccess.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    List<Message> findBySenderIdOrReceiverId(Long senderId, Long receiverId);
    
    List<Message> findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdOrderByTimestampAsc(
        Long senderId1, Long receiverId1, Long senderId2, Long receiverId2
    );
    
    List<Message> findByReceiverIdAndReadFalse(Long receiverId);
    
    Long countByReceiverIdAndReadFalse(Long receiverId);
}
