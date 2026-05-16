package com.itaccess.service;

import com.itaccess.dto.MessageDTO;
import com.itaccess.dto.MessageRequest;
import com.itaccess.entity.Message;
import com.itaccess.entity.User;
import com.itaccess.repository.MessageRepository;
import com.itaccess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    
    public List<MessageDTO> getAll() {
        return messageRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<MessageDTO> getConversation(Long currentUserId, Long otherUserId) {
        List<Message> messages = messageRepository.findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdOrderByTimestampAsc(
            currentUserId, otherUserId, currentUserId, otherUserId
        );
        return messages.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<MessageDTO> getUnreadMessages(Long userId) {
        return messageRepository.findByReceiverIdAndReadFalse(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public Long getUnreadCount(Long userId) {
        return messageRepository.countByReceiverIdAndReadFalse(userId);
    }
    
    public java.util.Map<Long, Long> getUnreadByUser(Long userId) {
        List<Message> unreadMessages = messageRepository.findByReceiverIdAndReadFalse(userId);
        return unreadMessages.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    Message::getSenderId,
                    java.util.stream.Collectors.counting()
                ));
    }
    
    @Transactional
    public MessageDTO create(MessageRequest request, Long senderId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));
        
        Message message = Message.builder()
                .senderId(senderId)
                .senderUsername(sender.getUsername())
                .receiverId(request.getReceiverId())
                .receiverUsername(receiver.getUsername())
                .content(request.getContent())
                .read(false)
                .build();
        
        return toDTO(messageRepository.save(message));
    }
    
    @Transactional
    public MessageDTO markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        message.setRead(true);
        return toDTO(messageRepository.save(message));
    }
    
    @Transactional
    public void delete(Long id) {
        messageRepository.deleteById(id);
    }
    
    private MessageDTO toDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .senderUsername(message.getSenderUsername())
                .receiverId(message.getReceiverId())
                .receiverUsername(message.getReceiverUsername())
                .content(message.getContent())
                .read(message.getRead())
                .timestamp(message.getTimestamp())
                .build();
    }
}
