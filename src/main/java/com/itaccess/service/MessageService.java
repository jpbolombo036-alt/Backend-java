package com.itaccess.service;

import com.itaccess.dto.MessageDTO;
import com.itaccess.dto.MessageRequest;
import com.itaccess.dto.SystemNotificationDTO;
import com.itaccess.dto.UnreadConversationDTO;
import com.itaccess.entity.Message;
import com.itaccess.entity.SystemNotification;
import com.itaccess.entity.User;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.MessageRepository;
import com.itaccess.repository.SystemNotificationRepository;
import com.itaccess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SystemNotificationRepository notificationRepository;
    
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
    
    public Map<Long, Long> getUnreadByUser(Long userId) {
        List<Message> unreadMessages = messageRepository.findByReceiverIdAndReadFalse(userId);
        return unreadMessages.stream()
                .collect(Collectors.groupingBy(
                    Message::getSenderId,
                    Collectors.counting()
                ));
    }
    
    public List<UnreadConversationDTO> getUnreadConversations(Long userId) {
        List<Message> unreadMessages = messageRepository.findByReceiverIdAndReadFalse(userId);
        return unreadMessages.stream()
                .collect(Collectors.groupingBy(Message::getSenderId))
                .entrySet()
                .stream()
                .map(entry -> {
                    Long senderId = entry.getKey();
                    List<Message> messages = entry.getValue();
                    messages.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
                    Message last = messages.get(0);
                    User sender = userRepository.findById(senderId).orElse(null);
                    String username = sender != null ? sender.getUsername() : "Unknown";
                    return UnreadConversationDTO.builder()
                            .userId(senderId)
                            .username(username)
                            .unreadCount((long) messages.size())
                            .lastMessage(toDTO(last))
                            .build();
                })
                .toList();
    }
    
    @Transactional
    public List<MessageDTO> markConversationAsRead(Long userId, Long otherUserId) {
        List<Message> unreadMessages = messageRepository.findByReceiverIdAndSenderIdAndReadFalse(userId, otherUserId);
        unreadMessages.forEach(m -> m.setRead(true));
        messageRepository.saveAll(unreadMessages);
        return unreadMessages.stream().map(this::toDTO).collect(Collectors.toList());
    }
    
    @Transactional
    public MessageDTO create(MessageRequest request, Long senderId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Expéditeur non trouvé avec l'ID: " + senderId));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Destinataire non trouvé avec l'ID: " + request.getReceiverId()));
        
        Message message = Message.builder()
                .senderId(senderId)
                .senderUsername(sender.getUsername())
                .receiverId(request.getReceiverId())
                .receiverUsername(receiver.getUsername())
                .content(request.getContent())
                .read(false)
                .build();
        
        Message saved = messageRepository.save(message);
        
        SystemNotification notif = SystemNotification.builder()
                .title("Nouveau message")
                .message(sender.getUsername() + " vous a envoyé un message")
                .type(SystemNotification.NotificationType.INFO)
                .targetUserId(receiver.getId())
                .createdBy(senderId)
                .actionUrl("/messages")
                .build();
        notificationRepository.save(notif);
        
        return toDTO(saved);
    }
    
    @Transactional
    public MessageDTO markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message non trouvé avec l'ID: " + messageId));
        
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
