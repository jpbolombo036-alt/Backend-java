package com.itaccess.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itaccess.dto.AiChatRequest;
import com.itaccess.dto.AiChatResponse;
import com.itaccess.entity.*;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.*;
import com.itaccess.security.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final CompteRepository compteRepository;
    private final TodoRepository todoRepository;
    private final TestSessionRepository testSessionRepository;
    private final TestRepository testRepository;
    private final BlocNoteRepository blocNoteRepository;
    private final DocumentArchiveRepository documentArchiveRepository;
    private final BugRepository bugRepository;
    private final AttendanceRepository attendanceRepository;
    private final ApkFileRepository apkFileRepository;
    private final MessageRepository messageRepository;
    private final SystemNotificationRepository systemNotificationRepository;
    private final ReportGenerationRepository reportGenerationRepository;
    private final HabilitationRepository habilitationRepository;
    private final AiConversationRepository aiConversationRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final AttachmentRepository attachmentRepository;
    private final ObjectMapper objectMapper;

    private final MessageService messageService;
    private final SystemNotificationService systemNotificationService;
    private final ReportService reportService;
    private final HabilitationService habilitationService;

    @Value("${app.openai.api-key:}")
    private String openAiApiKey;

    @Value("${app.openai.model:gpt-4o}")
    private String openAiModel;

    @Value("${app.openai.max-tokens:1000}")
    private int maxTokens;

    @Value("${app.openai.url:https://api.openai.com/v1/chat/completions}")
    private String openAiUrl;

    private static final String SYSTEM_PROMPT = """
            Tu es l'assistant IA de **IT Access Manager**, une application de gestion des accès IT.
            Tu aides les utilisateurs à naviguer, comprendre les données et répondre à leurs questions.
            
            Comportement :
            - Réponds TOUJOURS en français, sur un ton professionnel et concis.
            - Si la question est mal orthographiée ou ambiguë, reformule-la mentalement et réponds à la demande la plus probable.
            - Ne jamais inventer de données : si tu n'as pas accès à une information, dis-le clairement.
            - Vérifie toujours la cohérence avec le rôle et les droits de l'utilisateur connecté.
            - Si une action nécessite des droits admin, infirme calmement et propose l'alternative de lecture seule.
            - Format Markdown autorisé : listes, gras, code court.
            """;

    private static final int MAX_RETRIES = 2;
    private static final long BASE_BACKOFF_MS = 2000;
    private static final long MAX_BACKOFF_MS = 60000;
    private static final long DEFAULT_QUOTA_WAIT_SECONDS = 60;
    private static final Pattern RETRY_DELAY_PATTERN = Pattern.compile("retryDelay[\"\\s:]*(\\d+)s", Pattern.CASE_INSENSITIVE);
    private static final Set<String> GREETING_TOKENS = Set.of(
            "bonjour", "salut", "hello", "coucou", "hey", "bonsoir", "merci", "ok",
            "d'accord", "super", "au revoir", "ça", "ca", "va", "bien", "oui", "non", "ouais");

    private static final Map<String, List<String>> TOOL_KEYWORDS = new HashMap<>();
    static {
        TOOL_KEYWORDS.put("get_dashboard_stats", List.of("statistique", "dashboard", "tableau de bord", "résumé", "global", "vue d'ensemble", "combien", "aperçu"));
        TOOL_KEYWORDS.put("get_users", List.of("utilisateur", "admin", "liste des users", "qui est", "rôles", "comptes utilisateurs", "profils"));
        TOOL_KEYWORDS.put("get_applications", List.of("application", "apps", "logiciel", "appli"));
        TOOL_KEYWORDS.put("get_comptes", List.of("compte", "accès", "identifiant", "connexion"));
        TOOL_KEYWORDS.put("get_todos", List.of("tâche", "todo", "tâche à faire", "mes tâches", "à faire"));
        TOOL_KEYWORDS.put("get_test_sessions", List.of("session de test", "campagne", "test session", "campagnes"));
        TOOL_KEYWORDS.put("get_tests", List.of("résultat de test", "étapes de test", "tests", "défauts"));
        TOOL_KEYWORDS.put("get_bloc_notes", List.of("note", "bloc-note", "bloc note", "notes"));
        TOOL_KEYWORDS.put("get_documents", List.of("document", "archive", "fichier stocké", "docs"));
        TOOL_KEYWORDS.put("get_bugs", List.of("bug", "anomalie", "problème", "défaut", "bugs"));
        TOOL_KEYWORDS.put("get_attendances", List.of("présence", "pointage", "absence", "retard", "présences"));
        TOOL_KEYWORDS.put("get_attendances_stats", List.of("statistique de présence", "taux de présence", "pointages"));
        TOOL_KEYWORDS.put("get_latest_apk_files", List.of("apk", "android", "version mobile"));
        TOOL_KEYWORDS.put("create_todo", List.of("créer une tâche", "nouvelle tâche", "ajouter un todo", "ajouter une tâche", "créer un todo"));
        TOOL_KEYWORDS.put("toggle_todo_complete", List.of("terminer la tâche", "valider la tâche", "marquer comme terminé", "tâche faite", "cocher la tâche"));
        TOOL_KEYWORDS.put("update_todo", List.of("modifier la tâche", "changer le titre", "décaler la date", "mettre à jour la tâche", "éditer la tâche"));
        TOOL_KEYWORDS.put("delete_todo", List.of("supprimer la tâche", "effacer le todo", "retirer la tâche"));
        TOOL_KEYWORDS.put("create_bloc_note", List.of("créer une note", "nouvelle note", "ajouter une note"));
        TOOL_KEYWORDS.put("create_bug", List.of("déclarer un bug", "signaler un problème", "déclarer une anomalie", "créer un bug"));
        TOOL_KEYWORDS.put("update_bug", List.of("fermer le bug", "résoudre le bug", "modifier le bug", "statut du bug"));
        TOOL_KEYWORDS.put("update_test_status", List.of("modifier le statut du test", "changer le statut", "statut du test"));
        TOOL_KEYWORDS.put("create_test_session", List.of("créer une session de test", "lancer des tests", "nouvelle campagne", "démarrer une session"));
        TOOL_KEYWORDS.put("get_messages", List.of("message", "messagerie", "conversation", "messages"));
        TOOL_KEYWORDS.put("send_message", List.of("envoyer un message", "contacter", "message interne", "envoyer un msg"));
        TOOL_KEYWORDS.put("get_notifications", List.of("notification", "alerte", "notifications"));
        TOOL_KEYWORDS.put("get_unread_notifications_count", List.of("combien de notification", "alertes en attente", "non lues", "notifications non lues"));
        TOOL_KEYWORDS.put("generate_report", List.of("rapport", "report", "générer un rapport"));
        TOOL_KEYWORDS.put("get_habilitations", List.of("habilitation", "permission", "droits d'accès", "permissions"));
        TOOL_KEYWORDS.put("search", List.of("recherche", "cherche", "trouve", "rechercher"));
        TOOL_KEYWORDS.put("search_documents", List.of("rechercher dans les documents", "recherche document", "rechercher les documents"));
    }

    public AiChatResponse chat(String conversationId, List<AiChatRequest.AiMessage> messages, UserInfo currentUser) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return AiChatResponse.builder()
                    .error(true)
                    .errorMessage("Clé API OpenAI non configurée. Ajoutez OPENAI_API_KEY dans votre fichier .env")
                    .build();
        }

        AiConversation conversation = resolveConversation(conversationId, currentUser);
        persistUserMessages(conversation, messages);

        RestClient restClient = RestClient.create();
        String lastUserMessage = extractLastUserMessage(messages);
        Set<String> allowedTools = selectToolNames(lastUserMessage);
        String userId = currentUser != null ? String.valueOf(currentUser.getId()) : "anon";

        try {
            ObjectNode requestBody = buildRequestBody(messages, currentUser, allowedTools);
            String responseJson = postWithRetry(restClient, requestBody, userId);
            AiChatResponse response = processOpenAiResponse(responseJson, messages, restClient, currentUser);
            persistAssistantMessage(conversation, response.getReply());
            updateConversationTitle(conversation, lastUserMessage);
            response.setConversationId(conversation.getId());
            return response;

        } catch (AiCallException e) {
            if (e.statusCode == 429 || (e.statusCode >= 500 && e.statusCode < 600)) {
                long wait = (e.retryDelaySeconds != null) ? e.retryDelaySeconds : DEFAULT_QUOTA_WAIT_SECONDS;
                log.warn("Quota/erreur IA épuisé model={} userId={} status={} retryDelay={}s -> réponse dégradée",
                        openAiModel, userId, e.statusCode, e.retryDelaySeconds);
                if (e.statusCode == 429) {
                    AiChatResponse degraded = buildDegradedResponse(
                            "⚠️ Le quota de l'assistant IA est temporairement atteint (limite gratuite Gemini). "
                                    + "Réessayez dans environ " + wait + " secondes.", wait, currentUser);
                    persistAssistantMessage(conversation, degraded.getReply());
                    updateConversationTitle(conversation, lastUserMessage);
                    degraded.setConversationId(conversation.getId());
                    return degraded;
                }
                AiChatResponse degraded = buildDegradedResponse(
                        "Le service IA est temporairement indisponible (erreur " + e.statusCode
                                + "). Réessayez dans quelques instants.", wait, currentUser);
                persistAssistantMessage(conversation, degraded.getReply());
                updateConversationTitle(conversation, lastUserMessage);
                degraded.setConversationId(conversation.getId());
                return degraded;
            }
            log.error("Erreur lors de l'appel à l'IA model={} userId={} status={}", openAiModel, userId, e.statusCode, e);
            AiChatResponse errorResponse = AiChatResponse.builder()
                    .error(true)
                    .errorMessage("Erreur lors de la communication avec l'IA : " + e.getMessage())
                    .build();
            persistAssistantMessage(conversation, errorResponse.getErrorMessage());
            updateConversationTitle(conversation, lastUserMessage);
            errorResponse.setConversationId(conversation.getId());
            return errorResponse;
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'appel à l'IA model={} userId={}", openAiModel, userId, e);
            AiChatResponse errorResponse = AiChatResponse.builder()
                    .error(true)
                    .errorMessage("Erreur lors de la communication avec l'IA : " + e.getMessage())
                    .build();
            persistAssistantMessage(conversation, errorResponse.getErrorMessage());
            updateConversationTitle(conversation, lastUserMessage);
            errorResponse.setConversationId(conversation.getId());
            return errorResponse;
        }
    }

    private AiConversation resolveConversation(String conversationId, UserInfo currentUser) {
        if (conversationId != null && !conversationId.isBlank()) {
            try {
                Long id = Long.parseLong(conversationId);
                AiConversation conv = aiConversationRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Conversation IA non trouvée"));
                if (currentUser != null && !conv.getUserId().equals(currentUser.getId())) {
                    throw new SecurityException("Accès non autorisé à cette conversation");
                }
                return conv;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("conversationId invalide");
            }
        }
        AiConversation conversation = AiConversation.builder()
                .userId(currentUser != null ? currentUser.getId() : 0L)
                .title("Nouvelle conversation")
                .build();
        return aiConversationRepository.save(conversation);
    }

    private void persistUserMessages(AiConversation conversation, List<AiChatRequest.AiMessage> messages) {
        if (messages == null) return;
        for (AiChatRequest.AiMessage msg : messages) {
            if ("user".equalsIgnoreCase(msg.getRole()) && msg.getContent() != null) {
                AiChatMessage chatMessage = AiChatMessage.builder()
                        .conversation(conversation)
                        .role(msg.getRole())
                        .content(msg.getContent())
                        .build();
                aiChatMessageRepository.save(chatMessage);
            }
        }
    }

    private void persistAssistantMessage(AiConversation conversation, String content) {
        if (content == null || content.isBlank()) return;
        AiChatMessage chatMessage = AiChatMessage.builder()
                .conversation(conversation)
                .role("assistant")
                .content(content)
                .build();
        aiChatMessageRepository.save(chatMessage);
        conversation.setUpdatedAt(java.time.LocalDateTime.now());
        aiConversationRepository.save(conversation);
    }

    private void updateConversationTitle(AiConversation conversation, String firstUserMessage) {
        if (conversation.getTitle() == null || conversation.getTitle().equals("Nouvelle conversation")) {
            if (firstUserMessage != null && !firstUserMessage.isBlank()) {
                String title = firstUserMessage.length() > 80 ? firstUserMessage.substring(0, 80) + "..." : firstUserMessage;
                conversation.setTitle(title);
                aiConversationRepository.save(conversation);
            }
        }
    }

    public org.springframework.data.domain.Page<AiConversation> getConversations(Long userId, org.springframework.data.domain.Pageable pageable) {
        return aiConversationRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable);
    }

    public org.springframework.data.domain.Page<AiChatMessage> getConversationMessages(Long conversationId, org.springframework.data.domain.Pageable pageable) {
        return aiChatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);
    }

    public void deleteConversation(Long conversationId, Long userId) {
        AiConversation conversation = aiConversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation IA non trouvée"));
        if (!conversation.getUserId().equals(userId)) {
            throw new SecurityException("Accès non autorisé à cette conversation");
        }
        aiConversationRepository.delete(conversation);
    }

    public AiConversation renameConversation(Long conversationId, Long userId, String title) {
        AiConversation conversation = aiConversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation IA non trouvée"));
        if (!conversation.getUserId().equals(userId)) {
            throw new SecurityException("Accès non autorisé à cette conversation");
        }
        conversation.setTitle(title);
        return aiConversationRepository.save(conversation);
    }

    private ObjectNode buildRequestBody(List<AiChatRequest.AiMessage> messages, UserInfo currentUser, Set<String> allowedTools) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", openAiModel);
        body.put("max_tokens", maxTokens);

        ArrayNode messagesArray = objectMapper.createArrayNode();

        ObjectNode systemMsg = objectMapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
        messagesArray.add(systemMsg);

        if (currentUser != null) {
            ObjectNode contextMsg = objectMapper.createObjectNode();
            contextMsg.put("role", "system");
            contextMsg.put("content", "Contexte utilisateur : id=" + currentUser.getId() + ", username=" + currentUser.getUsername() + ", role=" + currentUser.getRole());
            messagesArray.add(contextMsg);
        }

        for (AiChatRequest.AiMessage msg : messages) {
            ObjectNode msgNode = objectMapper.createObjectNode();
            msgNode.put("role", msg.getRole());
            msgNode.put("content", msg.getContent());
            messagesArray.add(msgNode);
        }
        body.set("messages", messagesArray);

        ArrayNode tools = selectTools(buildTools(), allowedTools);
        if (tools.size() > 0) {
            body.set("tools", tools);
            body.put("tool_choice", "auto");
        }

        return body;
    }

    private ArrayNode selectTools(ArrayNode all, Set<String> allowed) {
        if (allowed == null || allowed.isEmpty()) return objectMapper.createArrayNode();
        ArrayNode out = objectMapper.createArrayNode();
        for (JsonNode tool : all) {
            String name = tool.path("function").path("name").asText();
            if (allowed.contains(name)) out.add(tool);
        }
        return out;
    }

    private String extractLastUserMessage(List<AiChatRequest.AiMessage> messages) {
        if (messages == null) return "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            AiChatRequest.AiMessage m = messages.get(i);
            if ("user".equalsIgnoreCase(m.getRole()) && m.getContent() != null) {
                return m.getContent();
            }
        }
        return "";
    }

    private Set<String> selectToolNames(String message) {
        if (message == null || message.isBlank()) return Set.of();
        String lower = message.toLowerCase();
        if (isGreetingOnly(lower)) return Set.of();

        Set<String> selected = new HashSet<>();
        selected.add("get_current_user");
        selected.add("get_user_context");
        selected.add("get_dashboard_stats");
        for (Map.Entry<String, List<String>> entry : TOOL_KEYWORDS.entrySet()) {
            for (String kw : entry.getValue()) {
                if (lower.contains(kw)) {
                    selected.add(entry.getKey());
                    break;
                }
            }
        }
        return selected;
    }

    private boolean isGreetingOnly(String lower) {
        String[] words = lower.split("\\W+");
        if (words.length == 0) return true;
        for (String w : words) {
            if (!GREETING_TOKENS.contains(w)) return false;
        }
        return true;
    }

    private AiChatResponse buildDegradedResponse(String intro, long wait, UserInfo currentUser) {
        StringBuilder sb = new StringBuilder();
        sb.append(intro).append("\n\n");
        sb.append("En attendant, voici un aperçu disponible sans appel à l'IA :\n\n");
        try {
            sb.append(formatDashboardStatsForFallback());
            if (currentUser != null) {
                sb.append("\nConnecté en tant que **").append(currentUser.getUsername())
                        .append("** (rôle : ").append(currentUser.getRole()).append(").");
            }
        } catch (Exception ex) {
            sb.append("_Données indisponibles._");
        }
        return AiChatResponse.builder()
                .error(true)
                .errorMessage("Assistant IA temporairement indisponible, réessayez dans environ " + wait + " secondes.")
                .reply(sb.toString())
                .model(openAiModel)
                .build();
    }

    private String formatDashboardStatsForFallback() throws Exception {
        JsonNode stats = objectMapper.readTree(getDashboardStats());
        StringBuilder sb = new StringBuilder();
        sb.append("- Utilisateurs : ").append(stats.path("total_utilisateurs").asInt()).append("\n");
        sb.append("- Applications : ").append(stats.path("total_applications").asInt()).append("\n");
        sb.append("- Comptes : ").append(stats.path("total_comptes").asInt()).append("\n");
        sb.append("- Tâches : ").append(stats.path("total_taches").asInt()).append("\n");
        sb.append("- Sessions de test : ").append(stats.path("total_sessions_test").asInt()).append("\n");
        sb.append("- Tests : ").append(stats.path("total_tests").asInt()).append("\n");
        sb.append("- Bugs : ").append(stats.path("total_bugs").asInt()).append("\n");
        sb.append("- Documents : ").append(stats.path("total_documents").asInt()).append("\n");
        return sb.toString();
    }

    private String postWithRetry(RestClient restClient, ObjectNode body, String userId) throws AiCallException {
        int attempt = 0;
        long backoff = BASE_BACKOFF_MS;
        while (true) {
            try {
                return restClient.post()
                        .uri(openAiUrl)
                        .header("Authorization", "Bearer " + openAiApiKey)
                        .header("Content-Type", "application/json")
                        .body(body.toString())
                        .retrieve()
                        .body(String.class);
            } catch (RestClientResponseException ex) {
                int code = ex.getStatusCode().value();
                String respBody = ex.getResponseBodyAsString();
                Long retryDelay = parseRetryDelay(respBody);
                boolean retriable = code == 429 || (code >= 500 && code < 600);
                if (retriable && attempt < MAX_RETRIES) {
                    long waitMs = Math.min((retryDelay != null ? retryDelay * 1000 : backoff), MAX_BACKOFF_MS);
                    log.warn("Appel IA en échec (status={}) model={} userId={} retryDelay={}s -> nouvel essai {} dans {}ms",
                            code, openAiModel, userId, retryDelay, (attempt + 1), waitMs);
                    sleepQuietly(waitMs);
                    backoff *= 2;
                    attempt++;
                    continue;
                }
                throw new AiCallException("Le fournisseur IA a répondu avec le statut " + code, code, respBody, retryDelay);
            } catch (RestClientException ex) {
                if (attempt < MAX_RETRIES) {
                    log.warn("Erreur réseau lors de l'appel IA model={} userId={} -> nouvel essai {} dans {}ms",
                            openAiModel, userId, (attempt + 1), backoff);
                    sleepQuietly(backoff);
                    backoff *= 2;
                    attempt++;
                    continue;
                }
                throw new AiCallException("Erreur réseau lors de l'appel à l'IA : " + ex.getMessage(), 0, null, null);
            }
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private Long parseRetryDelay(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                String msg = error.path("message").asText("");
                Matcher m = RETRY_DELAY_PATTERN.matcher(msg);
                if (m.find()) return Long.parseLong(m.group(1));
            }
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isMissingNode()) {
                Matcher m = RETRY_DELAY_PATTERN.matcher(content.asText());
                if (m.find()) return Long.parseLong(m.group(1));
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private static class AiCallException extends Exception {
        final int statusCode;
        final String body;
        final Long retryDelaySeconds;

        AiCallException(String message, int statusCode, String body, Long retryDelaySeconds) {
            super(message);
            this.statusCode = statusCode;
            this.body = body;
            this.retryDelaySeconds = retryDelaySeconds;
        }
    }

    private ArrayNode buildTools() {
        ArrayNode tools = objectMapper.createArrayNode();

        tools.add(buildTool("get_dashboard_stats",
                "Retourne les statistiques générales du dashboard : nombre d'utilisateurs, applications, comptes, tests, etc.",
                objectMapper.createObjectNode()));

        tools.add(buildTool("get_current_user",
                "Retourne les informations de l'utilisateur connecté : id, username, role. Utilise cet outil quand la question concerne 'moi', 'mon compte', 'mes droits' ou l'identité de l'utilisateur.",
                objectMapper.createObjectNode()));

        tools.add(buildTool("get_users",
                "Retourne la liste des utilisateurs de l'application avec leur rôle et statut. Utilise cet outil pour : 'qui est admin ?', 'liste des utilisateurs', 'combien d'utilisateurs ?', 'quels sont les rôles ?'.",
                objectMapper.createObjectNode()));

        tools.add(buildTool("get_applications",
                "Retourne la liste des applications IT gérées dans le système. Utilise cet outil pour : 'quelles applications ?', 'liste des apps', 'applications disponibles'.",
                objectMapper.createObjectNode()));

        tools.add(buildTool("get_comptes",
                "Retourne la liste des comptes d'accès associés aux applications. Utilise cet outil pour : 'quels comptes ?', 'accès par application', 'comptes stockés'.",
                objectMapper.createObjectNode()));

        ObjectNode todoProps = objectMapper.createObjectNode();
        ObjectNode statusProp = objectMapper.createObjectNode();
        statusProp.put("type", "string");
        statusProp.put("description", "Filtrer par statut : 'all', 'done', 'pending'. Par défaut 'all'.");
        todoProps.set("status", statusProp);
        tools.add(buildTool("get_todos",
                "Retourne la liste des tâches (todos) du système. Utilise cet outil pour : 'mes tâches', 'tâches en cours', 'tâches terminées', 'todo list'.",
                todoProps));

        tools.add(buildTool("get_test_sessions",
                "Retourne la liste des sessions de test avec leur statut et nombre de tests. Utilise cet outil pour : 'sessions de test', 'état des tests', 'campagnes de test'.",
                objectMapper.createObjectNode()));

        tools.add(buildTool("get_tests",
                "Retourne la liste des étapes de test individuelles (tests exécutés) avec leur statut. Utilise cet outil pour : 'résultats des tests', 'étapes de test', 'bugs dans les tests'.",
                objectMapper.createObjectNode()));

        tools.add(buildTool("get_bloc_notes",
                "Retourne la liste des bloc-notes créés dans l'application. Utilise cet outil pour : 'notes', 'bloc-notes', 'notes publiées', 'notes brouillon'.",
                objectMapper.createObjectNode()));

        tools.add(buildTool("get_documents",
                "Retourne la liste des documents archivés dans le système. Utilise cet outil pour : 'documents', 'archives', 'fichiers stockés', 'docs'.",
                objectMapper.createObjectNode()));

        tools.add(buildTool("get_bugs",
                "Retourne la liste globale des bugs déclarés dans le système. Utilise cet outil pour : 'bugs', 'anomalies', 'problèmes', 'défauts'.",
                objectMapper.createObjectNode()));

        ObjectNode attendanceProps = objectMapper.createObjectNode();
        ObjectNode attDateProp = objectMapper.createObjectNode();
        attDateProp.put("type", "string");
        attDateProp.put("description", "La date à interroger (ex: YYYY-MM-DD). Optionnel, par défaut aujourd'hui.");
        attendanceProps.set("date", attDateProp);
        ObjectNode attAgentProp = objectMapper.createObjectNode();
        attAgentProp.put("type", "integer");
        attAgentProp.put("description", "ID de l'agent spécifique pour filtrer sur les 7 derniers jours (optionnel).");
        attendanceProps.set("agentId", attAgentProp);
        tools.add(buildTool("get_attendances",
                "Retourne la liste des fiches de présence des agents (statut, check-in, check-out) pour un jour ou un agent. Utilise cet outil pour : 'présences', 'pointages', 'absences', 'retards'.",
                attendanceProps));

        tools.add(buildTool("get_attendances_stats",
                "Retourne les statistiques de présence du jour : total, présents, absents, retards. Utilise cet outil pour : 'statistiques de présence', 'taux de présence'.",
                objectMapper.createObjectNode()));

        tools.add(buildTool("get_latest_apk_files",
                "Retourne la liste des fichiers APK (versions d'applications mobiles) mis en ligne. Utilise cet outil pour : 'APK', 'versions mobiles', 'applications Android'.",
                objectMapper.createObjectNode()));

        ObjectNode createTodoProps = objectMapper.createObjectNode();
        ObjectNode titleProp = objectMapper.createObjectNode();
        titleProp.put("type", "string");
        titleProp.put("description", "Le titre de la tâche (obligatoire).");
        createTodoProps.set("title", titleProp);
        ObjectNode descProp = objectMapper.createObjectNode();
        descProp.put("type", "string");
        descProp.put("description", "La description de la tâche.");
        createTodoProps.set("description", descProp);
        ObjectNode priorityProp = objectMapper.createObjectNode();
        priorityProp.put("type", "string");
        priorityProp.put("description", "Priorité : 'normal', 'high', 'low'. Par défaut 'normal'.");
        createTodoProps.set("priority", priorityProp);
        ObjectNode dueDateProp = objectMapper.createObjectNode();
        dueDateProp.put("type", "string");
        dueDateProp.put("description", "Date d'échéance de la tâche (ex: YYYY-MM-DD).");
        createTodoProps.set("dueDate", dueDateProp);
        tools.add(buildTool("create_todo",
                "Crée une nouvelle tâche (todo) pour l'utilisateur connecté. Utilise cet outil pour : 'créer une tâche', 'nouvelle tâche', 'ajouter un todo'.",
                createTodoProps));

        ObjectNode toggleTodoProps = objectMapper.createObjectNode();
        ObjectNode todoIdProp = objectMapper.createObjectNode();
        todoIdProp.put("type", "integer");
        todoIdProp.put("description", "L'ID de la tâche à cocher/décoche (obligatoire).");
        toggleTodoProps.set("id", todoIdProp);
        tools.add(buildTool("toggle_todo_complete",
                "Bascule le statut d'une tâche (complétée ou non) par son ID. Utilise cet outil pour : 'marquer comme terminée', 'valider la tâche', 'tache faite'.",
                toggleTodoProps));

        ObjectNode updateTodoProps = objectMapper.createObjectNode();
        updateTodoProps.set("id", todoIdProp);
        ObjectNode newTitleProp = objectMapper.createObjectNode();
        newTitleProp.put("type", "string");
        newTitleProp.put("description", "Nouveau titre de la tâche (optionnel).");
        updateTodoProps.set("title", newTitleProp);
        ObjectNode newDescProp = objectMapper.createObjectNode();
        newDescProp.put("type", "string");
        newDescProp.put("description", "Nouvelle description de la tâche (optionnel).");
        updateTodoProps.set("description", newDescProp);
        ObjectNode newPriorityProp = objectMapper.createObjectNode();
        newPriorityProp.put("type", "string");
        newPriorityProp.put("description", "Nouvelle priorité : 'normal', 'high', 'low' (optionnel).");
        updateTodoProps.set("priority", newPriorityProp);
        ObjectNode newDueDateProp = objectMapper.createObjectNode();
        newDueDateProp.put("type", "string");
        newDueDateProp.put("description", "Nouvelle date d'échéance (ex: YYYY-MM-DD, optionnel).");
        updateTodoProps.set("dueDate", newDueDateProp);
        tools.add(buildTool("update_todo",
                "Met à jour une tâche existante par son ID. Utilise cet outil pour : 'modifier la tâche', 'changer le titre', 'décaler la date'.",
                updateTodoProps));

        ObjectNode deleteTodoProps = objectMapper.createObjectNode();
        deleteTodoProps.set("id", todoIdProp);
        tools.add(buildTool("delete_todo",
                "Supprime une tâche par son ID. Utilise cet outil pour : 'supprimer la tâche', 'effacer le todo'.",
                deleteTodoProps));

        ObjectNode createNoteProps = objectMapper.createObjectNode();
        ObjectNode noteTitleProp = objectMapper.createObjectNode();
        noteTitleProp.put("type", "string");
        noteTitleProp.put("description", "Le titre de la note (obligatoire).");
        createNoteProps.set("title", noteTitleProp);
        ObjectNode noteContentProp = objectMapper.createObjectNode();
        noteContentProp.put("type", "string");
        noteContentProp.put("description", "Le contenu textuel de la note (obligatoire).");
        createNoteProps.set("content", noteContentProp);
        ObjectNode noteStatusProp = objectMapper.createObjectNode();
        noteStatusProp.put("type", "string");
        noteStatusProp.put("description", "Statut : 'DRAFT', 'PUBLISHED'. Par défaut 'DRAFT'.");
        createNoteProps.set("status", noteStatusProp);
        ObjectNode noteAppIdProp = objectMapper.createObjectNode();
        noteAppIdProp.put("type", "integer");
        noteAppIdProp.put("description", "ID de l'application associée (optionnel).");
        createNoteProps.set("applicationId", noteAppIdProp);
        tools.add(buildTool("create_bloc_note",
                "Crée une nouvelle note dans le bloc-notes. Utilise cet outil pour : 'créer une note', 'nouvelle note', 'bloc-notes'.",
                createNoteProps));

        ObjectNode createBugProps = objectMapper.createObjectNode();
        ObjectNode bugTitleProp = objectMapper.createObjectNode();
        bugTitleProp.put("type", "string");
        bugTitleProp.put("description", "Titre court ou résumé du bug (obligatoire).");
        createBugProps.set("title", bugTitleProp);
        ObjectNode bugSeverityProp = objectMapper.createObjectNode();
        bugSeverityProp.put("type", "string");
        bugSeverityProp.put("description", "Gravité : 'CRITICAL', 'MAJOR', 'MINOR'. Par défaut 'MAJOR'.");
        createBugProps.set("severity", bugSeverityProp);
        ObjectNode bugPriorityProp = objectMapper.createObjectNode();
        bugPriorityProp.put("type", "string");
        bugPriorityProp.put("description", "Priorité : 'HIGH', 'MEDIUM', 'LOW'. Par défaut 'MEDIUM'.");
        createBugProps.set("priority", bugPriorityProp);
        ObjectNode bugReproProp = objectMapper.createObjectNode();
        bugReproProp.put("type", "string");
        bugReproProp.put("description", "Étapes de reproduction de l'anomalie.");
        createBugProps.set("reproducibility", bugReproProp);
        ObjectNode bugStepIdProp = objectMapper.createObjectNode();
        bugStepIdProp.put("type", "integer");
        bugStepIdProp.put("description", "ID de l'étape de test (TestStep) liée au bug (optionnel).");
        createBugProps.set("testStepId", bugStepIdProp);
        tools.add(buildTool("create_bug",
                "Déclare un nouveau bug lié à une anomalie. Utilise cet outil pour : 'déclarer un bug', 'signaler un problème', 'anomalie'.",
                createBugProps));

        ObjectNode updateBugProps = objectMapper.createObjectNode();
        ObjectNode bugIdProp = objectMapper.createObjectNode();
        bugIdProp.put("type", "integer");
        bugIdProp.put("description", "L'ID du bug (obligatoire).");
        updateBugProps.set("id", bugIdProp);
        ObjectNode bugStatusProp = objectMapper.createObjectNode();
        bugStatusProp.put("type", "string");
        bugStatusProp.put("description", "Nouveau statut : 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'.");
        updateBugProps.set("status", bugStatusProp);
        tools.add(buildTool("update_bug",
                "Met à jour le statut d'un bug existant. Utilise cet outil pour : 'fermer le bug', 'résoudre le bug', 'avancement bug'.",
                updateBugProps));

        ObjectNode createTestSessionProps = objectMapper.createObjectNode();
        ObjectNode sessionNameProp = objectMapper.createObjectNode();
        sessionNameProp.put("type", "string");
        sessionNameProp.put("description", "Nom de la session de test (obligatoire).");
        createTestSessionProps.set("nom", sessionNameProp);
        ObjectNode envProp = objectMapper.createObjectNode();
        envProp.put("type", "string");
        envProp.put("description", "Environnement de test : 'DEV', 'STAGING', 'PROD'.");
        createTestSessionProps.set("environnement", envProp);
        ObjectNode versionProp = objectMapper.createObjectNode();
        versionProp.put("type", "string");
        versionProp.put("description", "Version testée.");
        createTestSessionProps.set("version", versionProp);
        ObjectNode appIdProp = objectMapper.createObjectNode();
        appIdProp.put("type", "integer");
        appIdProp.put("description", "ID de l'application testée (optionnel).");
        createTestSessionProps.set("applicationId", appIdProp);
        ObjectNode platformProp = objectMapper.createObjectNode();
        platformProp.put("type", "string");
        platformProp.put("description", "Plateforme : 'Web', 'Android', 'iOS'. Par défaut 'Web'.");
        createTestSessionProps.set("plateforme", platformProp);
        tools.add(buildTool("create_test_session",
                "Crée une nouvelle session de test. Utilise cet outil pour : 'créer une session de test', 'nouvelle campagne', 'lancer des tests'.",
                createTestSessionProps));

        ObjectNode messageProps = objectMapper.createObjectNode();
        ObjectNode otherUserIdProp = objectMapper.createObjectNode();
        otherUserIdProp.put("type", "integer");
        otherUserIdProp.put("description", "ID de l'autre utilisateur pour une conversation (optionnel).");
        messageProps.set("userId", otherUserIdProp);
        tools.add(buildTool("get_messages",
                "Retourne les messages non lus ou une conversation selon le paramètre userId. Utilise cet outil pour : 'messages non lus', 'conversation avec X', 'messagerie'.",
                messageProps));

        ObjectNode sendMsgProps = objectMapper.createObjectNode();
        ObjectNode receiverIdProp = objectMapper.createObjectNode();
        receiverIdProp.put("type", "integer");
        receiverIdProp.put("description", "ID du destinataire (obligatoire).");
        sendMsgProps.set("receiverId", receiverIdProp);
        ObjectNode contentProp = objectMapper.createObjectNode();
        contentProp.put("type", "string");
        contentProp.put("description", "Contenu du message (obligatoire).");
        sendMsgProps.set("content", contentProp);
        tools.add(buildTool("send_message",
                "Envoie un message interne à un autre utilisateur. Utilise cet outil pour : 'envoyer un message', 'contacter X', 'message interne'.",
                sendMsgProps));

        ObjectNode notifProps = objectMapper.createObjectNode();
        ObjectNode notifTypeProp = objectMapper.createObjectNode();
        notifTypeProp.put("type", "string");
        notifTypeProp.put("description", "Filtrer par type : INFO, WARNING, ERROR. Optionnel.");
        notifProps.set("type", notifTypeProp);
        tools.add(buildTool("get_notifications",
                "Retourne les notifications de l'utilisateur connecté. Utilise cet outil pour : 'mes notifications', 'alertes', 'notifications non lues'.",
                notifProps));

        tools.add(buildTool("get_unread_notifications_count",
                "Retourne le nombre de notifications non lues pour l'utilisateur connecté. Utilise cet outil pour : 'combien de notifications ?', 'alertes en attente'.",
                objectMapper.createObjectNode()));

        ObjectNode reportProps = objectMapper.createObjectNode();
        ObjectNode reportTypeProp = objectMapper.createObjectNode();
        reportTypeProp.put("type", "string");
        reportTypeProp.put("description", "Type de rapport : 'SECURITY', 'ACCESS', 'TESTS', 'PERFORMANCE', 'COMPLIANCE'.");
        reportProps.set("reportType", reportTypeProp);
        tools.add(buildTool("generate_report",
                "Génère un rapport du type spécifié. Utilise cet outil pour : 'générer un rapport', 'rapport de sécurité', 'rapport de tests'.",
                reportProps));

        tools.add(buildTool("get_habilitations",
                "Retourne la liste des habilitations (permissions) des comptes d'accès. Utilise cet outil pour : 'habilitations', 'permissions', 'droits d'accès'.",
                objectMapper.createObjectNode()));

        ObjectNode searchProps = objectMapper.createObjectNode();
        ObjectNode queryProp = objectMapper.createObjectNode();
        queryProp.put("type", "string");
        queryProp.put("description", "Terme de recherche.");
        searchProps.set("query", queryProp);
        tools.add(buildTool("search",
                "Recherche plein texte dans les entités métier.",
                searchProps));

        tools.add(buildTool("get_user_context",
                "Retourne le contexte complet de l'utilisateur connecté : profil, todos assignés, notifications récentes et dernieres actions.",
                objectMapper.createObjectNode()));

        tools.add(buildTool("search_documents",
                "Recherche avancée dans les documents et notes par titre, contenu ou auteur.",
                searchProps));

        ObjectNode getAppByIdProps = new ObjectMapper().createObjectNode();
        ObjectNode getAppIdProp = new ObjectMapper().createObjectNode();
        getAppIdProp.put("type", "integer");
        getAppIdProp.put("description", "ID de l'application.");
        getAppByIdProps.set("id", getAppIdProp);
        tools.add(buildTool("get_application_by_id",
                "Retourne les détails d'une application par son ID.",
                getAppByIdProps));

        ObjectNode getCompteByIdProps = new ObjectMapper().createObjectNode();
        ObjectNode getCompteIdProp = new ObjectMapper().createObjectNode();
        getCompteIdProp.put("type", "integer");
        getCompteIdProp.put("description", "ID du compte d'accès.");
        getCompteByIdProps.set("id", getCompteIdProp);
        tools.add(buildTool("get_compte_by_id",
                "Retourne les détails d'un compte d'accès par son ID.",
                getCompteByIdProps));

        ObjectNode getBugByIdProps = new ObjectMapper().createObjectNode();
        ObjectNode getBugIdProp = new ObjectMapper().createObjectNode();
        getBugIdProp.put("type", "integer");
        getBugIdProp.put("description", "ID du bug.");
        getBugByIdProps.set("id", getBugIdProp);
        tools.add(buildTool("get_bug_by_id",
                "Retourne les détails d'un bug par son ID.",
                getBugByIdProps));

        ObjectNode getTodoByIdProps = new ObjectMapper().createObjectNode();
        ObjectNode getTodoIdProp = new ObjectMapper().createObjectNode();
        getTodoIdProp.put("type", "integer");
        getTodoIdProp.put("description", "ID de la tâche.");
        getTodoByIdProps.set("id", getTodoIdProp);
        tools.add(buildTool("get_todo_by_id",
                "Retourne les détails d'une tâche par son ID.",
                getTodoByIdProps));

        ObjectNode getSessionDetailsProps = new ObjectMapper().createObjectNode();
        ObjectNode getSessionIdProp = new ObjectMapper().createObjectNode();
        getSessionIdProp.put("type", "integer");
        getSessionIdProp.put("description", "ID de la session de test.");
        getSessionDetailsProps.set("id", getSessionIdProp);
        ObjectNode includeTestsProp = new ObjectMapper().createObjectNode();
        includeTestsProp.put("type", "boolean");
        includeTestsProp.put("description", "Inclure les étapes de test. Par défaut false.");
        getSessionDetailsProps.set("includeTests", includeTestsProp);
        tools.add(buildTool("get_test_session_details",
                "Retourne les détails d'une session de test, optionally avec ses étapes.",
                getSessionDetailsProps));

        ObjectNode getAttachmentsProps = new ObjectMapper().createObjectNode();
        ObjectNode getAttEntityIdProp = new ObjectMapper().createObjectNode();
        getAttEntityIdProp.put("type", "integer");
        getAttEntityIdProp.put("description", "ID de l'entité (compte, application, bug, etc.).");
        getAttachmentsProps.set("entityId", getAttEntityIdProp);
        ObjectNode getAttEntityTypeProp = new ObjectMapper().createObjectNode();
        getAttEntityTypeProp.put("type", "string");
        getAttEntityTypeProp.put("description", "Type d'entité : 'compte', 'application', 'bug', 'test_step'.");
        getAttachmentsProps.set("entityType", getAttEntityTypeProp);
        tools.add(buildTool("get_attachments",
                "Retourne les pièces jointes d'une entité par son ID et type.",
                getAttachmentsProps));

        ObjectNode createAppProps = new ObjectMapper().createObjectNode();
        ObjectNode createAppNameProp = new ObjectMapper().createObjectNode();
        createAppNameProp.put("type", "string");
        createAppNameProp.put("description", "Nom de l'application (obligatoire).");
        createAppProps.set("nom", createAppNameProp);
        ObjectNode createAppDescProp = new ObjectMapper().createObjectNode();
        createAppDescProp.put("type", "string");
        createAppDescProp.put("description", "Description de l'application.");
        createAppProps.set("description", createAppDescProp);
        ObjectNode createAppEnvProp = new ObjectMapper().createObjectNode();
        createAppEnvProp.put("type", "string");
        createAppEnvProp.put("description", "Environnement : 'DEV', 'STAGING', 'PROD'.");
        createAppProps.set("environnement", createAppEnvProp);
        tools.add(buildTool("create_application",
                "Crée une nouvelle application IT (admin uniquement).",
                createAppProps));

        ObjectNode updateAppProps = new ObjectMapper().createObjectNode();
        updateAppProps.set("id", getAppIdProp);
        ObjectNode updAppNameProp = new ObjectMapper().createObjectNode();
        updAppNameProp.put("type", "string");
        updAppNameProp.put("description", "Nouveau nom (optionnel).");
        updateAppProps.set("nom", updAppNameProp);
        ObjectNode updAppDescProp = new ObjectMapper().createObjectNode();
        updAppDescProp.put("type", "string");
        updAppDescProp.put("description", "Nouvelle description (optionnel).");
        updateAppProps.set("description", updAppDescProp);
        tools.add(buildTool("update_application",
                "Met à jour une application existante (admin uniquement).",
                updateAppProps));

        ObjectNode createCompteProps = new ObjectMapper().createObjectNode();
        createCompteProps.set("applicationId", getAppIdProp);
        ObjectNode createCompteUsernameProp = new ObjectMapper().createObjectNode();
        createCompteUsernameProp.put("type", "string");
        createCompteUsernameProp.put("description", "Nom d'utilisateur du compte (obligatoire).");
        createCompteProps.set("username", createCompteUsernameProp);
        ObjectNode createCompteCodeProp = new ObjectMapper().createObjectNode();
        createCompteCodeProp.put("type", "string");
        createCompteCodeProp.put("description", "Mot de passe ou code d'accès (obligatoire).");
        createCompteProps.set("code", createCompteCodeProp);
        ObjectNode createCompteRoleProp = new ObjectMapper().createObjectNode();
        createCompteRoleProp.put("type", "string");
        createCompteRoleProp.put("description", "Rôle du compte (optionnel).");
        createCompteProps.set("role", createCompteRoleProp);
        tools.add(buildTool("create_compte",
                "Crée un nouveau compte d'accès pour une application (admin uniquement).",
                createCompteProps));

        ObjectNode updateUserStatusProps = new ObjectMapper().createObjectNode();
        ObjectNode updateUserIdProp = new ObjectMapper().createObjectNode();
        updateUserIdProp.put("type", "integer");
        updateUserIdProp.put("description", "ID de l'utilisateur.");
        updateUserStatusProps.set("userId", updateUserIdProp);
        ObjectNode updateActiveProp = new ObjectMapper().createObjectNode();
        updateActiveProp.put("type", "boolean");
        updateActiveProp.put("description", "true pour activer, false pour désactiver.");
        updateUserStatusProps.set("active", updateActiveProp);
        tools.add(buildTool("update_user_status",
                "Active ou désactive un utilisateur (admin uniquement).",
                updateUserStatusProps));

        return tools;
    }

    private ObjectNode buildTool(String name, String description, ObjectNode properties) {
        ObjectNode tool = objectMapper.createObjectNode();
        ObjectNode function = objectMapper.createObjectNode();
        function.put("name", name);
        function.put("description", description);

        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        parameters.set("properties", properties);
        function.set("parameters", parameters);

        tool.put("type", "function");
        tool.set("function", function);
        return tool;
    }

    private AiChatResponse processOpenAiResponse(String responseJson,
                                                 List<AiChatRequest.AiMessage> messages,
                                                 RestClient restClient,
                                                 UserInfo currentUser) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode choice = root.path("choices").path(0);
        JsonNode message = choice.path("message");
        int tokensUsed = root.path("usage").path("total_tokens").asInt(0);

        JsonNode toolCalls = message.path("tool_calls");
        if (!toolCalls.isMissingNode() && toolCalls.isArray() && toolCalls.size() > 0) {
            return handleFunctionCalls(toolCalls, messages, tokensUsed, restClient, currentUser);
        }

        String reply = message.path("content").asText("Je n'ai pas pu générer une réponse.");
        return AiChatResponse.builder()
                .reply(reply)
                .tokensUsed(tokensUsed)
                .model(openAiModel)
                .build();
    }

    private AiChatResponse handleFunctionCalls(JsonNode toolCalls,
                                                 List<AiChatRequest.AiMessage> messages,
                                                 int tokensUsed,
                                                 RestClient restClient,
                                                 UserInfo currentUser) throws Exception {
        ArrayNode newMessages = objectMapper.createArrayNode();

        ObjectNode systemMsg = objectMapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
        newMessages.add(systemMsg);

        if (currentUser != null) {
            ObjectNode contextMsg = objectMapper.createObjectNode();
            contextMsg.put("role", "system");
            contextMsg.put("content", "Contexte utilisateur : id=" + currentUser.getId() + ", username=" + currentUser.getUsername() + ", role=" + currentUser.getRole());
            newMessages.add(contextMsg);
        }

        for (AiChatRequest.AiMessage msg : messages) {
            ObjectNode msgNode = objectMapper.createObjectNode();
            msgNode.put("role", msg.getRole());
            msgNode.put("content", msg.getContent());
            newMessages.add(msgNode);
        }

        ObjectNode assistantMsg = objectMapper.createObjectNode();
        assistantMsg.put("role", "assistant");
        assistantMsg.set("tool_calls", toolCalls);
        newMessages.add(assistantMsg);

        for (JsonNode toolCall : toolCalls) {
            String toolCallId = toolCall.path("id").asText();
            String functionName = toolCall.path("function").path("name").asText();
            String argsJson = toolCall.path("function").path("arguments").asText("{}");

            String functionResult = executeFunction(functionName, argsJson, currentUser);

            ObjectNode toolResultMsg = objectMapper.createObjectNode();
            toolResultMsg.put("role", "tool");
            toolResultMsg.put("tool_call_id", toolCallId);
            toolResultMsg.put("content", functionResult);
            newMessages.add(toolResultMsg);
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", openAiModel);
        body.put("max_tokens", maxTokens);
        body.set("messages", newMessages);

        String finalResponseJson;
        try {
            finalResponseJson = postWithRetry(restClient, body,
                    currentUser != null ? String.valueOf(currentUser.getId()) : "anon");
        } catch (AiCallException ex) {
            long wait = (ex.retryDelaySeconds != null) ? ex.retryDelaySeconds : DEFAULT_QUOTA_WAIT_SECONDS;
            log.warn("Échec après appels aux outils model={} status={} retryDelay={}s -> réponse dégradée",
                    openAiModel, ex.statusCode, ex.retryDelaySeconds);
            return buildDegradedResponse(
                    "J'ai récupéré des données via les outils métier, mais le service IA est indisponible pour "
                            + "finaliser la réponse (erreur " + ex.statusCode + "). Réessayez dans environ "
                            + wait + " secondes.", wait, currentUser);
        }

        JsonNode finalRoot = objectMapper.readTree(finalResponseJson);
        String reply = finalRoot.path("choices").path(0).path("message").path("content")
                .asText("Je n'ai pas pu générer une réponse.");
        int finalTokens = finalRoot.path("usage").path("total_tokens").asInt(0) + tokensUsed;

        return AiChatResponse.builder()
                .reply(reply)
                .tokensUsed(finalTokens)
                .model(openAiModel)
                .build();
    }

    private String executeFunction(String functionName, String argsJson, UserInfo currentUser) {
        try {
            return switch (functionName) {
                case "get_dashboard_stats" -> getDashboardStats();
                case "get_current_user" -> getCurrentUser(currentUser);
                case "get_users" -> getUsers();
                case "get_applications" -> getApplications();
                case "get_comptes" -> getComptes();
                case "get_todos" -> getTodos(argsJson);
                case "get_test_sessions" -> getTestSessions();
                case "get_tests" -> getTests(argsJson);
                case "get_bloc_notes" -> getBlocNotes();
                case "get_documents" -> getDocuments();
                case "get_bugs" -> getBugs();
                case "get_attendances" -> getAttendances(argsJson);
                case "get_attendances_stats" -> getAttendancesStats();
                case "get_latest_apk_files" -> getLatestApkFiles();
                case "create_todo" -> createTodo(argsJson, currentUser);
                case "toggle_todo_complete" -> toggleTodoComplete(argsJson, currentUser);
                case "update_todo" -> updateTodo(argsJson, currentUser);
                case "delete_todo" -> deleteTodo(argsJson, currentUser);
                case "create_bloc_note" -> createBlocNote(argsJson, currentUser);
                case "create_bug" -> createBug(argsJson, currentUser);
                case "update_bug" -> updateBug(argsJson, currentUser);
                case "update_test_status" -> updateTestStatus(argsJson, currentUser);
                case "create_test_session" -> createTestSession(argsJson, currentUser);
                case "get_messages" -> getMessages(argsJson, currentUser);
                case "send_message" -> sendMessage(argsJson, currentUser);
                case "get_notifications" -> getNotifications(currentUser);
                case "get_unread_notifications_count" -> getUnreadNotificationsCount(currentUser);
                case "generate_report" -> generateReport(argsJson, currentUser);
                case "get_habilitations" -> getHabilitations();
                case "search" -> search(argsJson);
                case "get_user_context" -> getUserContext(currentUser);
                case "search_documents" -> searchDocuments(argsJson);
                case "get_application_by_id" -> getApplicationById(argsJson);
                case "get_compte_by_id" -> getCompteById(argsJson);
                case "get_bug_by_id" -> getBugById(argsJson);
                case "get_todo_by_id" -> getTodoById(argsJson);
                case "get_test_session_details" -> getTestSessionDetails(argsJson);
                case "get_attachments" -> getAttachments(argsJson);
                case "create_application" -> createApplication(argsJson, currentUser);
                case "update_application" -> updateApplication(argsJson, currentUser);
                case "create_compte" -> createCompte(argsJson, currentUser);
                case "update_user_status" -> updateUserStatus(argsJson, currentUser);
                default -> "{\"error\": \"Fonction inconnue : " + functionName + "\"}";
            };
        } catch (Exception e) {
            log.error("Erreur lors de l'exécution de la fonction {}", functionName, e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private String getCurrentUser(UserInfo currentUser) throws Exception {
        if (currentUser == null) {
            return "{\"error\": \"Utilisateur non connecté\"}";
        }
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", currentUser.getId());
        node.put("username", currentUser.getUsername());
        node.put("role", currentUser.getRole());
        return objectMapper.writeValueAsString(node);
    }

    private String getDashboardStats() throws Exception {
        long users = userRepository.count();
        long applications = applicationRepository.count();
        long comptes = compteRepository.count();
        long todos = todoRepository.count();
        long testSessions = testSessionRepository.count();
        long tests = testRepository.count();
        long bugs = bugRepository.count();
        long documents = documentArchiveRepository.count();

        ObjectNode stats = objectMapper.createObjectNode();
        stats.put("total_utilisateurs", users);
        stats.put("total_applications", applications);
        stats.put("total_comptes", comptes);
        stats.put("total_taches", todos);
        stats.put("total_sessions_test", testSessions);
        stats.put("total_tests", tests);
        stats.put("total_bugs", bugs);
        stats.put("total_documents", documents);
        return objectMapper.writeValueAsString(stats);
    }

    private String getUsers() throws Exception {
        var users = userRepository.findAll().stream()
                .limit(50)
                .map(u -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", u.getId());
                    node.put("username", u.getUsername());
                    node.put("email", u.getEmail());
                    node.put("role", u.getRole());
                    node.put("actif", u.getIsActive() != null && u.getIsActive());
                    return node;
                })
                .toList();
        ObjectNode result = objectMapper.createObjectNode();
        result.put("total", users.size());
        result.set("utilisateurs", objectMapper.valueToTree(users));
        return objectMapper.writeValueAsString(result);
    }

    private String getApplications() throws Exception {
        var apps = applicationRepository.findAll().stream()
                .limit(50)
                .map(a -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", a.getId());
                    node.put("nom", a.getNom());
                    return node;
                })
                .toList();
        ObjectNode result = objectMapper.createObjectNode();
        result.put("total", apps.size());
        result.set("applications", objectMapper.valueToTree(apps));
        return objectMapper.writeValueAsString(result);
    }

    private String getComptes() throws Exception {
        var comptes = compteRepository.findAll().stream()
                .limit(50)
                .map(c -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", c.getId());
                    node.put("username", c.getUsername());
                    node.put("role", c.getRole());
                    if (c.getApplication() != null) {
                        node.put("application", c.getApplication().getNom());
                    }
                    return node;
                })
                .toList();
        ObjectNode result = objectMapper.createObjectNode();
        result.put("total", comptes.size());
        result.set("comptes", objectMapper.valueToTree(comptes));
        return objectMapper.writeValueAsString(result);
    }

    private String getTodos(String argsJson) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        String status = args.path("status").asText("all");
        List<Todo> todos = todoRepository.findAll();
        if (!"all".equalsIgnoreCase(status)) {
            boolean done = "done".equalsIgnoreCase(status);
            todos = todos.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getCompleted()) == done)
                    .toList();
        }
        var list = todos.stream()
                .limit(50)
                .map(t -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", t.getId());
                    node.put("titre", t.getTitle());
                    node.put("priorite", t.getPriority());
                    node.put("termine", t.getCompleted() != null && t.getCompleted());
                    node.put("echeance", t.getDueDate());
                    return node;
                })
                .toList();
        ObjectNode result = objectMapper.createObjectNode();
        result.put("total", list.size());
        result.set("taches", objectMapper.valueToTree(list));
        return objectMapper.writeValueAsString(result);
    }

    private String getTestSessions() throws Exception {
        var sessions = testSessionRepository.findAll().stream()
                .limit(30)
                .map(s -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", s.getId());
                    node.put("nom", s.getNom());
                    node.put("statut", s.getStatut());
                    node.put("environnement", s.getEnvironnement());
                    node.put("version", s.getVersion());
                    node.put("plateforme", s.getPlateforme());
                    if (s.getApplicationId() != null) {
                        Application app = applicationRepository.findById(s.getApplicationId()).orElse(null);
                        if (app != null) node.put("application", app.getNom());
                    }
                    return node;
                })
                .toList();
        ObjectNode result = objectMapper.createObjectNode();
        result.put("total", sessions.size());
        result.set("sessions", objectMapper.valueToTree(sessions));
        return objectMapper.writeValueAsString(result);
    }

    private String getTests(String argsJson) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        Long sessionId = args.has("sessionId") ? args.path("sessionId").asLong() : null;
        List<TestStep> tests = sessionId != null ? testRepository.findBySessionId(sessionId) : testRepository.findAll();
        var list = tests.stream()
                .limit(50)
                .map(t -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", t.getId());
                    node.put("session_id", t.getSessionId());
                    node.put("fonction", t.getFonction());
                    node.put("statut", t.getStatut());
                    node.put("resultat_attendu", t.getResultatAttendu());
                    node.put("resultat_obtenu", t.getResultatObtenu());
                    node.put("commentaires", t.getCommentaires());
                    node.put("resolu", t.getResolved());
                    return node;
                })
                .toList();
        ObjectNode result = objectMapper.createObjectNode();
        result.put("total", list.size());
        result.set("tests", objectMapper.valueToTree(list));
        return objectMapper.writeValueAsString(result);
    }

    private String getBlocNotes() throws Exception {
        var notes = blocNoteRepository.findAll().stream()
                .limit(20)
                .map(n -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", n.getId());
                    node.put("titre", n.getTitle());
                    node.put("auteur", n.getCreatedByUsername());
                    node.put("statut", n.getStatus());
                    return node;
                })
                .toList();
        ObjectNode result = objectMapper.createObjectNode();
        result.put("total", notes.size());
        result.set("notes", objectMapper.valueToTree(notes));
        return objectMapper.writeValueAsString(result);
    }

    private String getDocuments() throws Exception {
        var docs = documentArchiveRepository.findAll().stream()
                .limit(30)
                .map(d -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", d.getId());
                    node.put("titre", d.getTitle());
                    node.put("nom_fichier", d.getOriginalFileName());
                    node.put("type", d.getContentType());
                    node.put("categorie", d.getCategory());
                    node.put("auteur", d.getUploadedByUsername());
                    return node;
                })
                .toList();
        ObjectNode result = objectMapper.createObjectNode();
        result.put("total", docs.size());
        result.set("documents", objectMapper.valueToTree(docs));
        return objectMapper.writeValueAsString(result);
    }

    private String getBugs() throws Exception {
        var bugs = bugRepository.findAll().stream()
                .limit(30)
                .map(b -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", b.getId());
                    node.put("titre", b.getTitle());
                    node.put("gravite", b.getSeverity());
                    node.put("priorite", b.getPriority());
                    node.put("statut", b.getStatus());
                    return node;
                })
                .toList();
        ObjectNode result = objectMapper.createObjectNode();
        result.put("total", bugs.size());
        result.set("bugs", objectMapper.valueToTree(bugs));
        return objectMapper.writeValueAsString(result);
    }

    private String getAttendances(String argsJson) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        LocalDate date = args.has("date") ? LocalDate.parse(args.path("date").asText()) : LocalDate.now();
        Long agentId = args.has("agentId") ? args.path("agentId").asLong() : null;

        List<Attendance> attendances;
        if (agentId != null) {
            attendances = attendanceRepository.findByAgentIdAndDateBetween(agentId, date.minusDays(7), date);
        } else {
            attendances = attendanceRepository.findByDate(date);
        }

        var list = attendances.stream()
                .map(a -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", a.getId());
                    node.put("agentId", a.getAgentId());
                    node.put("agentUsername", a.getAgentUsername());
                    node.put("date", a.getDate().toString());
                    node.put("statut", a.getStatus());
                    node.put("arrivee", a.getCheckInTime() != null ? a.getCheckInTime().toString() : "-");
                    node.put("depart", a.getCheckOutTime() != null ? a.getCheckOutTime().toString() : "-");
                    node.put("motif", a.getReason() != null ? a.getReason() : "");
                    return node;
                })
                .toList();

        ObjectNode result = objectMapper.createObjectNode();
        result.put("total", list.size());
        result.set("attendances", objectMapper.valueToTree(list));
        return objectMapper.writeValueAsString(result);
    }

    private String getAttendancesStats() throws Exception {
        LocalDate today = LocalDate.now();
        List<Attendance> attendances = attendanceRepository.findByDate(today);
        long total = attendances.size();
        long present = attendances.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
        long absent = attendances.stream().filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus())).count();
        long late = attendances.stream().filter(a -> "LATE".equalsIgnoreCase(a.getStatus())).count();

        ObjectNode stats = objectMapper.createObjectNode();
        stats.put("date", today.toString());
        stats.put("total", total);
        stats.put("presents", present);
        stats.put("absents", absent);
        stats.put("retards", late);
        return objectMapper.writeValueAsString(stats);
    }

    private String getLatestApkFiles() throws Exception {
        var apks = apkFileRepository.findAll().stream()
                .limit(20)
                .map(a -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", a.getId());
                    node.put("nom_fichier", a.getOriginalFileName());
                    node.put("taille", a.getFileSize());
                    node.put("version", a.getVersion());
                    node.put("nom_paquet", a.getPackageName());
                    node.put("date_telechargement", a.getUploadDate() != null ? a.getUploadDate().toString() : "-");
                    node.put("description", a.getDescription() != null ? a.getDescription() : "");
                    return node;
                })
                .toList();

        ObjectNode result = objectMapper.createObjectNode();
        result.put("total", apks.size());
        result.set("apk_files", objectMapper.valueToTree(apks));
        return objectMapper.writeValueAsString(result);
    }

    private String createTodo(String argsJson, UserInfo currentUser) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        String title = args.path("title").asText(null);
        if (title == null || title.isBlank()) return "{\"error\": \"Le titre est obligatoire pour créer une tâche.\"}";
        String description = args.path("description").asText(null);
        String priority = args.path("priority").asText("normal");
        String dueDate = args.path("dueDate").asText(null);

        Todo todo = Todo.builder()
                .title(title)
                .description(description)
                .priority(priority)
                .dueDate(dueDate)
                .completed(false)
                .createdBy(currentUser.getId())
                .build();
        Todo saved = todoRepository.save(todo);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("message", "Tâche créée avec succès");
        result.put("id", saved.getId());
        result.put("title", saved.getTitle());
        result.put("completed", saved.getCompleted());
        return objectMapper.writeValueAsString(result);
    }

    private String toggleTodoComplete(String argsJson, UserInfo currentUser) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        long id = args.path("id").asLong(0);
        if (id <= 0) return "{\"error\": \"L'identifiant de la tâche (id) est obligatoire.\"}";

        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'ID: " + id));

        if (!"admin".equals(currentUser.getRole()) && !todo.getCreatedBy().equals(currentUser.getId())) {
            return "{\"error\": \"Non autorisé à modifier cette tâche.\"}";
        }

        todo.setCompleted(!todo.getCompleted());
        Todo saved = todoRepository.save(todo);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("message", "Statut de la tâche mis à jour");
        result.put("id", saved.getId());
        result.put("completed", saved.getCompleted());
        return objectMapper.writeValueAsString(result);
    }

    private String updateTodo(String argsJson, UserInfo currentUser) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        long id = args.path("id").asLong(0);
        if (id <= 0) return "{\"error\": \"L'identifiant de la tâche est obligatoire.\"}";

        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'ID: " + id));

        if (!"admin".equals(currentUser.getRole()) && !todo.getCreatedBy().equals(currentUser.getId())) {
            return "{\"error\": \"Non autorisé à modifier cette tâche.\"}";
        }

        if (args.has("title")) todo.setTitle(args.path("title").asText(null));
        if (args.has("description")) todo.setDescription(args.path("description").asText(null));
        if (args.has("priority")) todo.setPriority(args.path("priority").asText(null));
        if (args.has("dueDate")) todo.setDueDate(args.path("dueDate").asText(null));
        if (args.has("completed")) todo.setCompleted(args.path("completed").asBoolean(false));

        Todo saved = todoRepository.save(todo);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("message", "Tâche mise à jour");
        result.put("id", saved.getId());
        result.put("title", saved.getTitle());
        result.put("completed", saved.getCompleted());
        return objectMapper.writeValueAsString(result);
    }

    private String deleteTodo(String argsJson, UserInfo currentUser) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        long id = args.path("id").asLong(0);
        if (id <= 0) return "{\"error\": \"L'identifiant de la tâche est obligatoire.\"}";

        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'ID: " + id));

        if (!"admin".equals(currentUser.getRole()) && !todo.getCreatedBy().equals(currentUser.getId())) {
            return "{\"error\": \"Non autorisé à supprimer cette tâche.\"}";
        }

        todoRepository.delete(todo);
        return "{\"success\": true, \"message\": \"Tâche supprimée\", \"id\": \" + id + \"}";
    }

    private String createBlocNote(String argsJson, UserInfo currentUser) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        String title = args.path("title").asText(null);
        String content = args.path("content").asText(null);
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            return "{\"error\": \"Le titre et le contenu sont obligatoires pour créer une note.\"}";
        }
        String status = args.path("status").asText("DRAFT");
        Long appId = args.has("applicationId") ? args.path("applicationId").asLong() : null;

        BlocNote note = BlocNote.builder()
                .title(title)
                .content(content)
                .status(status)
                .applicationId(appId)
                .createdBy(currentUser.getId())
                .createdByUsername(currentUser.getUsername())
                .build();

        BlocNote saved = blocNoteRepository.save(note);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("message", "Note créée avec succès");
        result.put("id", saved.getId());
        result.put("title", saved.getTitle());
        return objectMapper.writeValueAsString(result);
    }

    private String createBug(String argsJson, UserInfo currentUser) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        String title = args.path("title").asText(null);
        if (title == null || title.isBlank()) return "{\"error\": \"Le titre du bug est obligatoire.\"}";
        String severity = args.path("severity").asText("MAJOR");
        String priority = args.path("priority").asText("MEDIUM");
        String reproducibility = args.path("reproducibility").asText(null);
        Long testStepId = args.has("testStepId") ? args.path("testStepId").asLong() : null;

        Bug bug = Bug.builder()
                .title(title)
                .severity(severity)
                .priority(priority)
                .reproducibility(reproducibility)
                .testStepId(testStepId)
                .status("OPEN")
                .assignedTo(currentUser.getId())
                .build();

        Bug saved = bugRepository.save(bug);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("message", "Bug déclaré avec succès");
        result.put("id", saved.getId());
        result.put("title", saved.getTitle());
        return objectMapper.writeValueAsString(result);
    }

    private String updateBug(String argsJson, UserInfo currentUser) throws Exception {
        if (!"admin".equals(currentUser.getRole())) return "{\"error\": \"Seul un administrateur peut modifier le statut d'un bug.\"}";
        JsonNode args = objectMapper.readTree(argsJson);
        long id = args.path("id").asLong(0);
        String status = args.path("status").asText(null);
        if (id <= 0 || status == null || status.isBlank()) return "{\"error\": \"ID et nouveau statut sont obligatoires.\"}";

        Bug bug = bugRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bug non trouvé : " + id));
        bug.setStatus(status);
        Bug saved = bugRepository.save(bug);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("message", "Statut du bug mis à jour");
        result.put("id", saved.getId());
        result.put("statut", saved.getStatus());
        return objectMapper.writeValueAsString(result);
    }

    private String updateTestStatus(String argsJson, UserInfo currentUser) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        long id = args.path("id").asLong(0);
        String status = args.path("status").asText(null);
        if (id <= 0 || status == null || status.isBlank()) return "{\"error\": \"ID et nouveau statut sont obligatoires.\"}";

        TestStep test = testRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test non trouvé avec l'ID: " + id));

        if (!"admin".equals(currentUser.getRole()) && !test.getCreatedBy().equals(currentUser.getId())) {
            return "{\"error\": \"Non autorisé à modifier ce test.\"}";
        }

        test.setStatut(status);
        TestStep saved = testRepository.save(test);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("message", "Statut du test mis à jour");
        result.put("id", saved.getId());
        result.put("statut", saved.getStatut());
        return objectMapper.writeValueAsString(result);
    }

    private String createTestSession(String argsJson, UserInfo currentUser) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        String nom = args.path("nom").asText(null);
        if (nom == null || nom.isBlank()) return "{\"error\": \"Le nom de la session est obligatoire.\"}";
        String environnement = args.path("environnement").asText("DEV");
        Long applicationId = args.has("applicationId") ? args.path("applicationId").asLong() : null;

        TestSession session = TestSession.builder()
                .nom(nom)
                .description(args.path("description").asText(null))
                .applicationId(applicationId)
                .environnement(environnement)
                .version(args.path("version").asText(null))
                .statut("OPEN")
                .plateforme(args.path("plateforme").asText("Web"))
                .createdBy(currentUser.getId())
                .build();

        TestSession saved = testSessionRepository.save(session);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("message", "Session de test créée avec succès");
        result.put("id", saved.getId());
        result.put("nom", saved.getNom());
        return objectMapper.writeValueAsString(result);
    }

    private String getMessages(String argsJson, UserInfo currentUser) throws Exception {
        if (currentUser == null) return "[]";
        JsonNode args = objectMapper.readTree(argsJson);
        Long otherUserId = args.has("userId") ? args.path("userId").asLong() : null;

        List<Message> messages;
        if (otherUserId != null) {
            messages = messageRepository.findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdOrderByTimestampAsc(
                    currentUser.getId(), otherUserId, currentUser.getId(), otherUserId);
        } else {
            messages = messageRepository.findByReceiverIdAndReadFalse(currentUser.getId());
            messages.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        }

        var list = messages.stream()
                .limit(30)
                .map(m -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", m.getId());
                    node.put("sender_id", m.getSenderId());
                    node.put("sender_username", m.getSenderUsername());
                    node.put("receiver_id", m.getReceiverId());
                    node.put("contenu", m.getContent());
                    node.put("lu", m.getRead());
                    node.put("date", m.getTimestamp() != null ? m.getTimestamp().toString() : "");
                    return node;
                })
                .toList();
        return objectMapper.writeValueAsString(list);
    }

    private String sendMessage(String argsJson, UserInfo currentUser) throws Exception {
        if (currentUser == null) return "{\"error\": \"Utilisateur non connecté\"}";
        JsonNode args = objectMapper.readTree(argsJson);
        Long receiverId = args.path("receiverId").asLong();
        String content = args.path("content").asText(null);
        if (content == null || content.isBlank()) return "{\"error\": \"Le contenu du message est obligatoire\"}";

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Destinataire non trouvé"));

        Message message = Message.builder()
                .senderId(currentUser.getId())
                .senderUsername(currentUser.getUsername())
                .receiverId(receiverId)
                .receiverUsername(receiver.getUsername())
                .content(content)
                .read(false)
                .build();

        Message saved = messageRepository.save(message);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("message", "Message envoyé");
        result.put("id", saved.getId());
        result.put("senderUsername", saved.getSenderUsername());
        result.put("receiverUsername", saved.getReceiverUsername());
        return objectMapper.writeValueAsString(result);
    }

    private String getNotifications(UserInfo currentUser) throws Exception {
        if (currentUser == null) return "[]";
        var notifs = systemNotificationRepository.findAllVisibleByUserId(currentUser.getId()).stream()
                .limit(20)
                .map(n -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", n.getId());
                    node.put("titre", n.getTitle());
                    node.put("message", n.getMessage());
                    node.put("type", n.getType().name());
                    node.put("lu", n.getRead());
                    node.put("date", n.getCreatedAt() != null ? n.getCreatedAt().toString() : "");
                    return node;
                })
                .toList();
        return objectMapper.writeValueAsString(notifs);
    }

    private String getUnreadNotificationsCount(UserInfo currentUser) throws Exception {
        if (currentUser == null) return "{\"count\": 0}";
        long count = systemNotificationRepository.countUnreadByUserId(currentUser.getId());
        ObjectNode result = objectMapper.createObjectNode();
        result.put("count", count);
        return objectMapper.writeValueAsString(result);
    }

    private String getHabilitations() throws Exception {
        var habilitations = habilitationRepository.findAll().stream()
                .limit(50)
                .map(h -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", h.getId());
                    node.put("compteId", h.getCompteId());
                    node.put("permission", h.getPermission());
                    return node;
                })
                .toList();
        ObjectNode result = objectMapper.createObjectNode();
        result.put("total", habilitations.size());
        result.set("habilitations", objectMapper.valueToTree(habilitations));
        return objectMapper.writeValueAsString(result);
    }

    private String search(String argsJson) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        String query = args.path("query").asText(null);
        if (query == null || query.isBlank()) return "{\"error\": \"La requête de recherche est obligatoire.\"}";
        String lower = query.toLowerCase();
        List<ObjectNode> results = new ArrayList<>();

        todoRepository.findAll().stream().limit(20).forEach(t -> {
            if ((t.getTitle() != null && t.getTitle().toLowerCase().contains(lower)) ||
                (t.getDescription() != null && t.getDescription().toLowerCase().contains(lower))) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("type", "todo");
                node.put("id", t.getId());
                node.put("titre", t.getTitle());
                node.put("priorite", t.getPriority());
                node.put("termine", t.getCompleted() != null && t.getCompleted());
                results.add(node);
            }
        });

        blocNoteRepository.findAll().stream().limit(20).forEach(n -> {
            if ((n.getTitle() != null && n.getTitle().toLowerCase().contains(lower)) ||
                (n.getContent() != null && n.getContent().toLowerCase().contains(lower))) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("type", "bloc_note");
                node.put("id", n.getId());
                node.put("titre", n.getTitle());
                node.put("auteur", n.getCreatedByUsername());
                node.put("statut", n.getStatus());
                results.add(node);
            }
        });

        bugRepository.findAll().stream().limit(20).forEach(b -> {
            if ((b.getTitle() != null && b.getTitle().toLowerCase().contains(lower)) ||
                (b.getReproducibility() != null && b.getReproducibility().toLowerCase().contains(lower))) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("type", "bug");
                node.put("id", b.getId());
                node.put("titre", b.getTitle());
                node.put("gravite", b.getSeverity());
                node.put("priorite", b.getPriority());
                node.put("statut", b.getStatus());
                results.add(node);
            }
        });

        testRepository.findAll().stream().limit(20).forEach(t -> {
            if ((t.getFonction() != null && t.getFonction().toLowerCase().contains(lower)) ||
                (t.getCommentaires() != null && t.getCommentaires().toLowerCase().contains(lower))) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("type", "test");
                node.put("id", t.getId());
                node.put("fonction", t.getFonction());
                node.put("statut", t.getStatut());
                node.put("sessionId", t.getSessionId());
                results.add(node);
            }
        });

        ObjectNode result = objectMapper.createObjectNode();
        result.put("query", lower);
        result.put("total", results.size());
        result.set("results", objectMapper.valueToTree(results));
        return objectMapper.writeValueAsString(result);
    }

    private String generateReport(String argsJson, UserInfo currentUser) throws Exception {
        if (currentUser == null) return "{\"error\": \"Utilisateur non connecté\"}";
        JsonNode args = objectMapper.readTree(argsJson);
        String typeId = args.path("reportType").asText(null);
        if (typeId == null || typeId.isBlank()) return "{\"error\": \"Le type de rapport est obligatoire.\"}";

        com.itaccess.dto.ReportGenerationDTO report = reportService.generate(typeId, currentUser);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("id", report.getId());
        result.put("title", report.getTitle());
        result.put("reportType", report.getReportType());
        result.put("status", report.getStatus());
        result.put("generatedAt", report.getGeneratedAt() != null ? report.getGeneratedAt().toString() : null);
        result.put("generatedByUsername", report.getGeneratedByUsername());
        return objectMapper.writeValueAsString(result);
    }

    private String getUserContext(UserInfo currentUser) throws Exception {
        if (currentUser == null) return "{\"error\": \"Utilisateur non connecté\"}";

        ObjectNode context = objectMapper.createObjectNode();
        context.put("id", currentUser.getId());
        context.put("username", currentUser.getUsername());
        context.put("role", currentUser.getRole());

        var todos = todoRepository.findAll().stream()
                .filter(t -> currentUser.getId().equals(t.getCreatedBy()))
                .limit(10)
                .map(t -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", t.getId());
                    node.put("titre", t.getTitle());
                    node.put("termine", t.getCompleted() != null && t.getCompleted());
                    node.put("priorite", t.getPriority());
                    node.put("echeance", t.getDueDate());
                    return node;
                })
                .toList();
        context.set("todos", objectMapper.valueToTree(todos));

        var notifications = systemNotificationRepository.findAllVisibleByUserId(currentUser.getId()).stream()
                .limit(10)
                .map(n -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", n.getId());
                    node.put("titre", n.getTitle());
                    node.put("message", n.getMessage());
                    node.put("lu", n.getRead());
                    node.put("date", n.getCreatedAt() != null ? n.getCreatedAt().toString() : "");
                    return node;
                })
                .toList();
        context.set("notifications", objectMapper.valueToTree(notifications));

        long unread = systemNotificationRepository.countUnreadByUserId(currentUser.getId());
        context.put("notifications_non_lues", unread);

        return objectMapper.writeValueAsString(context);
    }

    private String searchDocuments(String argsJson) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        String query = args.path("query").asText(null);
        if (query == null || query.isBlank()) return "{\"error\": \"La requête de recherche est obligatoire.\"}";
        String lower = query.toLowerCase();
        List<ObjectNode> results = new ArrayList<>();

        documentArchiveRepository.findAll().stream().limit(30).forEach(d -> {
            boolean match = (d.getTitle() != null && d.getTitle().toLowerCase().contains(lower)) ||
                            (d.getDescription() != null && d.getDescription().toLowerCase().contains(lower)) ||
                            (d.getUploadedByUsername() != null && d.getUploadedByUsername().toLowerCase().contains(lower));
            if (match) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("type", "document");
                node.put("id", d.getId());
                node.put("titre", d.getTitle());
                node.put("categorie", d.getCategory());
                node.put("auteur", d.getUploadedByUsername());
                results.add(node);
            }
        });

        blocNoteRepository.findAll().stream().limit(30).forEach(n -> {
            boolean match = (n.getTitle() != null && n.getTitle().toLowerCase().contains(lower)) ||
                            (n.getContent() != null && n.getContent().toLowerCase().contains(lower)) ||
                            (n.getCreatedByUsername() != null && n.getCreatedByUsername().toLowerCase().contains(lower));
            if (match) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("type", "note");
                node.put("id", n.getId());
                node.put("titre", n.getTitle());
                node.put("auteur", n.getCreatedByUsername());
                node.put("statut", n.getStatus());
                results.add(node);
            }
        });

        ObjectNode result = objectMapper.createObjectNode();
        result.put("query", lower);
        result.put("total", results.size());
        result.set("results", objectMapper.valueToTree(results));
        return objectMapper.writeValueAsString(result);
    }

    private String getApplicationById(String argsJson) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        Long id = args.path("id").asLong(0);
        if (id == 0) return "{\"error\": \"ID application requis.\"}";
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application non trouvée"));
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", app.getId());
        node.put("nom", app.getNom());
        node.put("description", app.getDescription());
        node.put("version", app.getVersion());
        node.put("environnement", app.getEnvironnement());
        node.put("dateCreation", app.getDateCreation() != null ? app.getDateCreation().toString() : null);
        node.put("createdBy", app.getCreatedBy());
        return objectMapper.writeValueAsString(node);
    }

    private String getCompteById(String argsJson) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        Long id = args.path("id").asLong(0);
        if (id == 0) return "{\"error\": \"ID compte requis.\"}";
        Compte compte = compteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé"));
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", compte.getId());
        node.put("applicationId", compte.getApplicationId());
        node.put("username", compte.getUsername());
        node.put("role", compte.getRole());
        node.put("commentaire", compte.getCommentaire());
        node.put("createdBy", compte.getCreatedBy());
        return objectMapper.writeValueAsString(node);
    }

    private String getBugById(String argsJson) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        Long id = args.path("id").asLong(0);
        if (id == 0) return "{\"error\": \"ID bug requis.\"}";
        Bug bug = bugRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bug non trouvé"));
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", bug.getId());
        node.put("title", bug.getTitle());
        node.put("severity", bug.getSeverity());
        node.put("priority", bug.getPriority());
        node.put("status", bug.getStatus());
        node.put("reproducibility", bug.getReproducibility());
        node.put("assignedTo", bug.getAssignedTo());
        node.put("createdAt", bug.getCreatedAt() != null ? bug.getCreatedAt().toString() : null);
        return objectMapper.writeValueAsString(node);
    }

    private String getTodoById(String argsJson) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        Long id = args.path("id").asLong(0);
        if (id == 0) return "{\"error\": \"ID tâche requis.\"}";
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée"));
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", todo.getId());
        node.put("title", todo.getTitle());
        node.put("description", todo.getDescription());
        node.put("priority", todo.getPriority());
        node.put("completed", todo.getCompleted() != null && todo.getCompleted());
        node.put("dueDate", todo.getDueDate() != null ? todo.getDueDate().toString() : null);
        node.put("createdBy", todo.getCreatedBy());
        return objectMapper.writeValueAsString(node);
    }

    private String getTestSessionDetails(String argsJson) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        Long id = args.path("id").asLong(0);
        boolean includeTests = args.path("includeTests").asBoolean(false);
        if (id == 0) return "{\"error\": \"ID session requis.\"}";
        TestSession session = testSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session de test non trouvée"));
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", session.getId());
        node.put("nom", session.getNom());
        node.put("environnement", session.getEnvironnement());
        node.put("version", session.getVersion());
        node.put("statut", session.getStatut());
        node.put("plateforme", session.getPlateforme());
        node.put("dateCreation", session.getDateCreation() != null ? session.getDateCreation().toString() : null);
        node.put("createdBy", session.getCreatedBy());

        if (includeTests) {
            var tests = testRepository.findBySessionId(id).stream().limit(50).map(t -> {
                ObjectNode tn = objectMapper.createObjectNode();
                tn.put("id", t.getId());
                tn.put("fonction", t.getFonction());
                tn.put("statut", t.getStatut());
                tn.put("commentaires", t.getCommentaires());
                return tn;
            }).toList();
            node.set("tests", objectMapper.valueToTree(tests));
        }
        return objectMapper.writeValueAsString(node);
    }

    private String getAttachments(String argsJson) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        Long entityId = args.path("entityId").asLong(0);
        String entityType = args.path("entityType").asText(null);
        if (entityId == 0 || entityType == null) return "{\"error\": \"entityId et entityType requis.\"}";
        List<Attachment> attachments;
        switch (entityType) {
            case "bug" -> attachments = attachmentRepository.findByBugId(entityId);
            case "test_step" -> attachments = attachmentRepository.findByTestStepId(entityId);
            case "message" -> attachments = attachmentRepository.findByMessageId(entityId);
            default -> attachments = List.of();
        }
        List<ObjectNode> results = attachments.stream().limit(20).map(a -> {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", a.getId());
            node.put("fileName", a.getFileName());
            node.put("fileSize", a.getFileSize());
            node.put("contentType", a.getContentType());
            node.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
            return node;
        }).toList();
        ObjectNode result = objectMapper.createObjectNode();
        result.put("entityType", entityType);
        result.put("entityId", entityId);
        result.put("total", results.size());
        result.set("attachments", objectMapper.valueToTree(results));
        return objectMapper.writeValueAsString(result);
    }

    private String createApplication(String argsJson, UserInfo currentUser) throws Exception {
        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getRole())) {
            return "{\"error\": \"Droits admin requis.\"}";
        }
        JsonNode args = objectMapper.readTree(argsJson);
        String nom = args.path("nom").asText(null);
        if (nom == null || nom.isBlank()) return "{\"error\": \"Le nom de l'application est obligatoire.\"}";
        Application app = Application.builder()
                .nom(nom)
                .description(args.path("description").asText(null))
                .environnement(args.path("environnement").asText(null))
                .createdBy(currentUser.getId())
                .build();
        Application saved = applicationRepository.save(app);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("id", saved.getId());
        result.put("nom", saved.getNom());
        return objectMapper.writeValueAsString(result);
    }

    private String updateApplication(String argsJson, UserInfo currentUser) throws Exception {
        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getRole())) {
            return "{\"error\": \"Droits admin requis.\"}";
        }
        JsonNode args = objectMapper.readTree(argsJson);
        Long id = args.path("id").asLong(0);
        if (id == 0) return "{\"error\": \"ID application requis.\"}";
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application non trouvée"));
        if (args.has("nom") && !args.path("nom").isMissingNode()) app.setNom(args.path("nom").asText());
        if (args.has("description") && !args.path("description").isMissingNode()) app.setDescription(args.path("description").asText(null));
        if (args.has("environnement") && !args.path("environnement").isMissingNode()) app.setEnvironnement(args.path("environnement").asText(null));
        Application saved = applicationRepository.save(app);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("id", saved.getId());
        result.put("nom", saved.getNom());
        return objectMapper.writeValueAsString(result);
    }

    private String createCompte(String argsJson, UserInfo currentUser) throws Exception {
        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getRole())) {
            return "{\"error\": \"Droits admin requis.\"}";
        }
        JsonNode args = objectMapper.readTree(argsJson);
        Long applicationId = args.path("applicationId").asLong(0);
        String username = args.path("username").asText(null);
        String code = args.path("code").asText(null);
        if (applicationId == 0 || username == null || username.isBlank() || code == null || code.isBlank()) {
            return "{\"error\": \"applicationId, username et code sont obligatoires.\"}";
        }
        Compte compte = Compte.builder()
                .applicationId(applicationId)
                .username(username)
                .code(code)
                .role(args.path("role").asText(null))
                .commentaire(args.path("commentaire").asText(null))
                .createdBy(currentUser.getId())
                .build();
        Compte saved = compteRepository.save(compte);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("id", saved.getId());
        result.put("username", saved.getUsername());
        return objectMapper.writeValueAsString(result);
    }

    private String updateUserStatus(String argsJson, UserInfo currentUser) throws Exception {
        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getRole())) {
            return "{\"error\": \"Droits admin requis.\"}";
        }
        JsonNode args = objectMapper.readTree(argsJson);
        Long userId = args.path("userId").asLong(0);
        boolean active = args.path("active").asBoolean(true);
        if (userId == 0) return "{\"error\": \"userId requis.\"}";
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        user.setIsActive(active);
        userRepository.save(user);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("userId", user.getId());
        result.put("active", user.getIsActive());
        return objectMapper.writeValueAsString(result);
    }
}