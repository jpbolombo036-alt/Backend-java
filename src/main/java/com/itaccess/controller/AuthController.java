package com.itaccess.controller;

import com.itaccess.dto.*;
import com.itaccess.entity.User;
import com.itaccess.repository.UserRepository;
import com.itaccess.security.JwtTokenProvider;
import com.itaccess.service.EmailService;
import com.itaccess.service.SettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "")
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final SettingService settingService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    
    @PostMapping(value = "/token", consumes = {"application/json", "application/x-www-form-urlencoded"})
    @Operation(summary = "Connexion", description = "Authentifie l'utilisateur et retourne un token JWT")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody(required = false) LoginRequest requestBody,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password) {
        
        String user = username;
        String pass = password;
        
        if (requestBody != null) {
            user = requestBody.getUsername();
            pass = requestBody.getPassword();
        }
        
        if (user == null || pass == null || user.isBlank() || pass.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(TokenResponse.builder()
                            .accessToken("Nom d'utilisateur et mot de passe requis")
                            .tokenType("bearer")
                            .build());
        }
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user, pass)
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            User existingUser = userRepository.findByUsername(user)
                    .orElse(null);
            
            if (existingUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(TokenResponse.builder()
                                .accessToken("Utilisateur non trouvé")
                                .tokenType("bearer")
                                .build());
            }
            
            if (existingUser.getIsActive() != null && !existingUser.getIsActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(TokenResponse.builder()
                                .accessToken("Compte désactivé. Veuillez contacter l'administrateur.")
                                .tokenType("bearer")
                                .build());
            }
            
            String token = jwtTokenProvider.generateToken(authentication);
            
            return ResponseEntity.ok(TokenResponse.builder()
                    .accessToken(token)
                    .tokenType("bearer")
                    .build());
            
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(TokenResponse.builder()
                            .accessToken("Nom d'utilisateur ou mot de passe incorrect")
                            .tokenType("bearer")
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(TokenResponse.builder()
                            .accessToken("Erreur: " + e.getMessage())
                            .tokenType("bearer")
                            .build());
        }
    }
    
    @GetMapping("/me")
    @Operation(summary = "Utilisateur actuel", description = "Retourne les informations de l'utilisateur actuellement connecté")
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        return ResponseEntity.ok(CurrentUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .build());
    }
    
    @PostMapping("/refresh-secret-key")
    public ResponseEntity<String> refreshSecretKey() {
        settingService.refreshSecretKey();
        return ResponseEntity.ok("SECRET_KEY vérifié/régénéré avec succès");
    }
    
    @PostMapping("/forgot-password")
    @Operation(summary = "Demande de réinitialisation", description = "Envoie un email de réinitialisation de mot de passe")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            // Générer un token temporaire (en production, utiliser un token UUID stocké en DB avec expiration)
            String resetToken = jwtTokenProvider.generateResetToken(user.getUsername());
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetToken);
        });
        
        return ResponseEntity.ok("Si cet email existe, un lien de réinitialisation a été envoyé");
    }
    
    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialiser le mot de passe", description = "Réinitialise le mot de passe avec le token reçu")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody PasswordResetConfirm confirm) {
        try {
            String username = jwtTokenProvider.validateResetToken(confirm.getToken());
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Token invalide ou expiré"));
            
            user.setHashedPassword(passwordEncoder.encode(confirm.getNewPassword()));
            userRepository.save(user);
            
            return ResponseEntity.ok("Mot de passe réinitialisé avec succès");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Token invalide ou expiré");
        }
    }
    
    @PostMapping("/init-admin")
    @Operation(summary = "Initialiser l'admin", description = "Crée l'utilisateur admin initial (à utiliser uniquement pour la première configuration)")
    public ResponseEntity<String> initAdmin(@RequestParam(required = false) String initKey) {
        // Clé de sécurité pour éviter les créations non autorisées
        String secureInitKey = "IT_ACCESS_INIT_2024_SECURE_KEY_CHANGE_ME";
        
        if (initKey == null || !initKey.equals(secureInitKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Clé d'initialisation invalide");
        }
        
        // Vérifier si un admin existe déjà
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(user -> "admin".equals(user.getRole()));
        
        if (adminExists) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Un admin existe déjà");
        }
        
        // Créer l'admin par défaut
        User admin = User.builder()
                .username("admin")
                .email("admin@itaccess.com")
                .role("admin")
                .isActive(true)
                .hashedPassword(passwordEncoder.encode("admin123"))
                .build();
        
        userRepository.save(admin);
        
        return ResponseEntity.ok("Admin initial créé avec succès. Username: admin, Password: admin123. CHANGEZ CE MOT DE PASSE IMMÉDIATEMENT !");
    }
    
    @PostMapping("/activate-user")
    @Operation(summary = "Activer un utilisateur", description = "Active un utilisateur par son username (réservé aux admins)")
    public ResponseEntity<String> activateUser(@RequestParam String username) {
        // Vérifier que l'utilisateur actuel est un admin
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !hasAdminRole(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Accès refusé : droits administrateur requis");
        }
        
        User user = userRepository.findByUsername(username)
                .orElse(null);
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Utilisateur non trouvé");
        }
        
        user.setIsActive(true);
        userRepository.save(user);
        
        return ResponseEntity.ok("Utilisateur " + username + " activé avec succès");
    }
    
    private boolean hasAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "admin".equals(auth.getAuthority()));
    }
}