package com.itaccess.controller;

import com.itaccess.dto.AiChatRequest;
import com.itaccess.dto.AiChatResponse;
import com.itaccess.entity.AiChatMessage;
import com.itaccess.entity.AiConversation;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "Agent IA conversationnel avec accès aux données de l'application")
public class AiController {

    private final AiService aiService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

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

        AiChatResponse response = aiService.chat(request.getConversationId(), request.getMessages(), currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "Envoyer un message à l'agent IA (streaming SSE)",
        description = "Même endpoint que /ai/chat mais retourne la réponse en streaming SSE."
    )
    public SseEmitter chatStream(
            @CurrentUser UserInfo currentUser,
            @RequestBody AiChatRequest request) {

        if (currentUser == null) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("Authentification requise"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            emitter.complete();
            return emitter;
        }

        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("La liste de messages ne peut pas être vide"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            emitter.complete();
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(300_000L);
        executor.execute(() -> streamChatResponse(emitter, request, currentUser));
        return emitter;
    }

    private void streamChatResponse(SseEmitter emitter, AiChatRequest request, UserInfo currentUser) {
        try {
            AiChatResponse response = aiService.chat(request.getConversationId(), request.getMessages(), currentUser);
            String reply = response.getReply() != null ? response.getReply() : "";
            String[] sentences = reply.split("(?<=[.!?])\\s+");
            StringBuilder sb = new StringBuilder();
            for (String sentence : sentences) {
                sb.append(sentence).append(" ");
                emitter.send(SseEmitter.event().name("chunk").data(sb.toString()));
                Thread.sleep(30);
            }
            emitter.send(SseEmitter.event().name("done").data(response));
            emitter.complete();
        } catch (Exception ex) {
            try {
                emitter.send(SseEmitter.event().name("error").data("Erreur: " + ex.getMessage()));
            } catch (Exception ignored) {
            }
            emitter.completeWithError(ex);
        }
    }

    @GetMapping("/conversations")
    @Operation(summary = "Liste des conversations IA", description = "Retourne les conversations de l'utilisateur connecté (paginées)")
    public ResponseEntity<Page<AiConversation>> getConversations(
            @CurrentUser UserInfo currentUser,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(aiService.getConversations(currentUser.getId(), pageable));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Messages d'une conversation", description = "Retourne les messages d'une conversation IA (paginés)")
    public ResponseEntity<Page<AiChatMessage>> getConversationMessages(
            @PathVariable Long conversationId,
            @CurrentUser UserInfo currentUser,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(aiService.getConversationMessages(conversationId, pageable));
    }

    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "Supprimer une conversation", description = "Supprime une conversation IA et ses messages")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable Long conversationId,
            @CurrentUser UserInfo currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        aiService.deleteConversation(conversationId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/conversations/{conversationId}")
    @Operation(summary = "Renommer une conversation", description = "Met à jour le titre d'une conversation IA")
    public ResponseEntity<AiConversation> renameConversation(
            @PathVariable Long conversationId,
            @CurrentUser UserInfo currentUser,
            @RequestBody Map<String, String> body) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        String title = body.get("title");
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(aiService.renameConversation(conversationId, currentUser.getId(), title));
    }
}
