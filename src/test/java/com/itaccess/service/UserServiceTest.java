package com.itaccess.service;

import com.itaccess.dto.UserDTO;
import com.itaccess.entity.User;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .hashedPassword("encodedPassword")
                .role("user")
                .isActive(true)
                .build();
    }
    
    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        UserDTO result = userService.getUserById(1L);
        
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).findById(1L);
    }
    
    @Test
    void getUserById_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(1L));
        verify(userRepository, times(1)).findById(1L);
    }
    
    @Test
    void createUser_ShouldReturnUser_WhenValidData() {
        UserDTO userDTO = UserDTO.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .role("user")
                .build();
        
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        UserDTO result = userService.createUser(userDTO);
        
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    void createUser_ShouldThrowException_WhenUsernameExists() {
        UserDTO userDTO = UserDTO.builder()
                .username("testuser")
                .email("new@example.com")
                .password("password123")
                .build();
        
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(userDTO));
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void createUser_ShouldThrowException_WhenEmailExists() {
        UserDTO userDTO = UserDTO.builder()
                .username("newuser")
                .email("test@example.com")
                .password("password123")
                .build();
        
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(userDTO));
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void deleteUser_ShouldDeleteUser_WhenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);
        
        userService.deleteUser(1L);
        
        verify(userRepository, times(1)).deleteById(1L);
    }
    
    @Test
    void deleteUser_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);
        
        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(1L));
        verify(userRepository, never()).deleteById(anyLong());
    }
}
