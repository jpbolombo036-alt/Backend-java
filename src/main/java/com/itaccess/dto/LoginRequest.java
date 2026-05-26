package com.itaccess.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    @NotBlank(message = "Nom d'utilisateur requis")
    private String username;
    @NotBlank(message = "Mot de passe requis")
    private String password;
    private String phoneVersion; // Exemple: "Android 14" ou "iOS 17.4"
}