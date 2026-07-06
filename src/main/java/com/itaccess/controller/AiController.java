package com.itaccess.controller;

import com.itaccess.dto.AiChatRequest;
import com.itaccess.dto.AiChatResponse;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "Agent IA conversationnel avec accès aux données de l'application")
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    @Operation(
        summary = "Envoyer un message à l'agent IA",
        description = "Envoie un message à GPT-4o avec l'historique de la conversation. L'agent peut accéder aux données de l'application via Function Calling."
    )
    public ResponseEntity<AiChatResponse> chat(
            @CurrentUser UserInfo currentUser,
            @RequestBody AiChatRequest request) {

        if (currentUser == null) {
            return ResponseEntity.status(401).body(
                AiChatResponse.builder()
                    .error(true)
                    .errorMessage("Authentification requise")
                    .build()
            );
        }

        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return ResponseEntity.badRequest().body(
                AiChatResponse.builder()
                    .error(true)
                    .errorMessage("La liste de messages ne peut pas être vide")
                    .build()
            );
        }

        AiChatResponse response = aiService.chat(request.getMessages());
        return ResponseEntity.ok(response);
    }
}
