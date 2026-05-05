package com.itaccess.service;

import com.itaccess.dto.PageResponse;
import com.itaccess.dto.UserDTO;
import com.itaccess.entity.User;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public PageResponse<UserDTO> getAllUsers(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> userPage = userRepository.findAll(pageable);
        
        List<UserDTO> content = userPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return PageResponse.of(content, userPage.getNumber(), userPage.getSize(), userPage.getTotalElements());
    }
    
    public List<UserDTO> getAvailableUsers(Long currentUserId) {
        return userRepository.findAll().stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));
        return toDTO(user);
    }
    
    @Transactional
    public UserDTO createUser(UserDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Nom d'utilisateur déjà enregistré");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email déjà enregistré");
        }
        
        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .hashedPassword(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole() != null ? dto.getRole() : "user")
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
        
        User savedUser = userRepository.save(user);
        return toDTO(savedUser);
    }
    
    @Transactional
    public UserDTO updateUser(Long id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));
        
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("Email déjà enregistré");
            }
            user.setEmail(dto.getEmail());
        }
        
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        
        if (dto.getIsActive() != null) {
            user.setIsActive(dto.getIsActive());
        }
        
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setHashedPassword(passwordEncoder.encode(dto.getPassword()));
        }
        
        User updatedUser = userRepository.save(user);
        return toDTO(updatedUser);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id);
        }
        userRepository.deleteById(id);
    }
    
    @Transactional
    public UserDTO updateUserProfile(Long id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));
        
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("Email déjà enregistré");
            }
            user.setEmail(dto.getEmail());
        }
        
        if (dto.getProfilePhoto() != null) {
            user.setProfilePhoto(dto.getProfilePhoto());
        }
        
        User updatedUser = userRepository.save(user);
        return toDTO(updatedUser);
    }
    
    @Transactional
    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));
        
        if (!passwordEncoder.matches(oldPassword, user.getHashedPassword())) {
            throw new IllegalArgumentException("Ancien mot de passe incorrect");
        }
        
        user.setHashedPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    public UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .profilePhoto(user.getProfilePhoto())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }
}