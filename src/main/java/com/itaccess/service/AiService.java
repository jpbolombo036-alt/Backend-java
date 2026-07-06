package com.itaccess.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itaccess.dto.AiChatRequest;
import com.itaccess.dto.AiChatResponse;
import com.itaccess.repository.*;
import com.itaccess.security.UserInfo;
import com.itaccess.entity.Todo;
import com.itaccess.entity.BlocNote;
import com.itaccess.entity.Bug;
import com.itaccess.entity.Attendance;
import com.itaccess.entity.ApkFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.time.LocalDate;

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
    private final ObjectMapper objectMapper;

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
            Tu aides les utilisateurs à naviguer dans l'application, comprendre les données et répondre à leurs questions.
            
            Tu as accès à des fonctions pour récupérer des données en temps réel :
            - Utilisateurs (liste, nombre, rôles)
            - Applications IT gérées
            - Comptes d'accès par application
            - Tâches (todos)
            - Sessions de test
            - Bloc-notes
            - Archive documentaire
            
            Règles importantes :
            - Réponds TOUJOURS en français
            - Sois concis, professionnel et bienveillant
            - Si tu n'as pas accès à une donnée, dis-le clairement
            - Utilise les fonctions disponibles pour répondre avec des données réelles
            - Format Markdown autorisé (listes, gras, code)
            """;

    public AiChatResponse chat(List<AiChatRequest.AiMessage> messages, UserInfo currentUser) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return AiChatResponse.builder()
                    .error(true)
                    .errorMessage("Clé API OpenAI non configurée. Ajoutez OPENAI_API_KEY dans votre fichier .env")
                    .build();
        }

        try {
            RestClient restClient = RestClient.create();
            ObjectNode requestBody = buildRequestBody(messages);

            String responseJson = restClient.post()
                    .uri(openAiUrl)
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .retrieve()
                    .body(String.class);

            return processOpenAiResponse(responseJson, messages, restClient, currentUser);

        } catch (Exception e) {
            log.error("Erreur lors de l'appel à OpenAI", e);
            return AiChatResponse.builder()
                    .error(true)
                    .errorMessage("Erreur lors de la communication avec l'IA : " + e.getMessage())
                    .build();
        }
    }

    private ObjectNode buildRequestBody(List<AiChatRequest.AiMessage> messages) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", openAiModel);
        body.put("max_tokens", maxTokens);

        // Messages
        ArrayNode messagesArray = objectMapper.createArrayNode();

        // System message
        ObjectNode systemMsg = objectMapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
        messagesArray.add(systemMsg);

        // User/Assistant messages
        for (AiChatRequest.AiMessage msg : messages) {
            ObjectNode msgNode = objectMapper.createObjectNode();
            msgNode.put("role", msg.getRole());
            msgNode.put("content", msg.getContent());
            messagesArray.add(msgNode);
        }
        body.set("messages", messagesArray);

        // Functions (Function Calling)
        body.set("tools", buildTools());
        body.put("tool_choice", "auto");

        return body;
    }

    private ArrayNode buildTools() {
        ArrayNode tools = objectMapper.createArrayNode();

        tools.add(buildTool("get_dashboard_stats",
                "Retourne les statistiques générales du dashboard : nombre d'utilisateurs, applications, comptes, tests, etc.",
                objectMapper.createObjectNode()));

        tools.add(buildTool("get_users",
                "Retourne la liste des utilisateurs de l'application avec leur rôle et statut.",
                objectMapper.createObjectNode()));

        tools.add(buildTool("get_applications",
                "Retourne la liste des applications IT gérées dans le système.",
                objectMapper.createObjectNode()));

        tools.add(buildTool("get_comptes",
                "Retourne la liste des comptes d'accès associés aux applications.",
                objectMapper.createObjectNode()));

        ObjectNode todoProps = objectMapper.createObjectNode();
        ObjectNode statusProp = objectMapper.createObjectNode();
        statusProp.put("type", "string");
        statusProp.put("description", "Filtrer par statut : 'all', 'done', 'pending'. Par défaut 'all'.");
        todoProps.set("status", statusProp);
        tools.add(buildTool("get_todos",
                "Retourne la liste des tâches (todos) du système.",
                todoProps));

        tools.add(buildTool("get_test_sessions",
                "Retourne la liste des sessions de test avec leur statut et nombre de tests.",
                objectMapper.createObjectNode()));

        tools.add(buildTool("get_bloc_notes",
                "Retourne la liste des bloc-notes créés dans l'application.",
                objectMapper.createObjectNode()));

        tools.add(buildTool("get_documents",
                "Retourne la liste des documents archivés dans le système.",
                objectMapper.createObjectNode()));

        // --- NEW WRITE / MUTATIVE TOOLS ---

        // 1. Create Todo
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
                "Crée une nouvelle tâche (todo) pour l'utilisateur connecté.",
                createTodoProps));

        // 2. Toggle Todo Complete
        ObjectNode toggleTodoProps = objectMapper.createObjectNode();
        ObjectNode idProp = objectMapper.createObjectNode();
        idProp.put("type", "integer");
        idProp.put("description", "L'ID de la tâche à cocher/décoche (obligatoire).");
        toggleTodoProps.set("id", idProp);
        tools.add(buildTool("toggle_todo_complete",
                "Bascule le statut d'une tâche (complétée ou non) par son ID.",
                toggleTodoProps));

        // 3. Create Bloc Note
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
                "Crée une nouvelle note dans le bloc-notes.",
                createNoteProps));

        // 4. Create Bug
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
                "Déclare un nouveau bug lié à une anomalie.",
                createBugProps));

        // 5. Get Bugs
        tools.add(buildTool("get_bugs",
                "Retourne la liste globale des bugs déclarés dans le système.",
                objectMapper.createObjectNode()));

        // 6. Get Attendances
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
                "Retourne la liste des fiches de présence des agents (statut, check-in, check-out) pour un jour ou un agent.",
                attendanceProps));

        // 7. Get Latest APK Files
        tools.add(buildTool("get_latest_apk_files",
                "Retourne la liste des fichiers APK (versions d'applications mobiles) mis en ligne.",
                objectMapper.createObjectNode()));

        return tools;
    }

    private ObjectNode buildTool(String name, String description, ObjectNode properties) {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("type", "function");

        ObjectNode function = objectMapper.createObjectNode();
        function.put("name", name);
        function.put("description", description);

        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        parameters.set("properties", properties);
        function.set("parameters", parameters);

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

        // Check if GPT wants to call a function
        JsonNode toolCalls = message.path("tool_calls");
        if (!toolCalls.isMissingNode() && toolCalls.isArray() && toolCalls.size() > 0) {
            return handleFunctionCalls(toolCalls, messages, tokensUsed, restClient, currentUser);
        }

        // Direct text response
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
        // Build a new message list with function results
        ArrayNode newMessages = objectMapper.createArrayNode();

        // Add system prompt
        ObjectNode systemMsg = objectMapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
        newMessages.add(systemMsg);

        // Add all previous messages
        for (AiChatRequest.AiMessage msg : messages) {
            ObjectNode msgNode = objectMapper.createObjectNode();
            msgNode.put("role", msg.getRole());
            msgNode.put("content", msg.getContent());
            newMessages.add(msgNode);
        }

        // Add assistant message with tool calls
        ObjectNode assistantMsg = objectMapper.createObjectNode();
        assistantMsg.put("role", "assistant");
        assistantMsg.set("tool_calls", toolCalls);
        newMessages.add(assistantMsg);

        // Execute each function call and add results
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

        // Second call to GPT with function results
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", openAiModel);
        body.put("max_tokens", maxTokens);
        body.set("messages", newMessages);

        String finalResponseJson = restClient.post()
                .uri(openAiUrl)
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .retrieve()
                .body(String.class);

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
                case "get_users" -> getUsers();
                case "get_applications" -> getApplications();
                case "get_comptes" -> getComptes();
                case "get_todos" -> getTodos(argsJson);
                case "get_test_sessions" -> getTestSessions();
                case "get_bloc_notes" -> getBlocNotes();
                case "get_documents" -> getDocuments();
                case "create_todo" -> createTodo(argsJson, currentUser);
                case "toggle_todo_complete" -> toggleTodoComplete(argsJson, currentUser);
                case "create_bloc_note" -> createBlocNote(argsJson, currentUser);
                case "create_bug" -> createBug(argsJson, currentUser);
                case "get_bugs" -> getBugs();
                case "get_attendances" -> getAttendances(argsJson);
                case "get_latest_apk_files" -> getLatestApkFiles();
                default -> "{\"error\": \"Fonction inconnue : " + functionName + "\"}";
            };
        } catch (Exception e) {
            log.error("Erreur lors de l'exécution de la fonction {}", functionName, e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private String getDashboardStats() throws Exception {
        long users = userRepository.count();
        long applications = applicationRepository.count();
        long comptes = compteRepository.count();
        long todos = todoRepository.count();
        long testSessions = testSessionRepository.count();
        long tests = testRepository.count();

        ObjectNode stats = objectMapper.createObjectNode();
        stats.put("total_utilisateurs", users);
        stats.put("total_applications", applications);
        stats.put("total_comptes", comptes);
        stats.put("total_taches", todos);
        stats.put("total_sessions_test", testSessions);
        stats.put("total_tests", tests);
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
        var todos = todoRepository.findAll().stream()
                .limit(50)
                .map(t -> {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", t.getId());
                    node.put("titre", t.getTitle());
                    node.put("priorite", t.getPriority());
                    node.put("termine", t.getCompleted() != null && t.getCompleted());
                    return node;
                })
                .toList();
        ObjectNode result = objectMapper.createObjectNode();
        result.put("total", todos.size());
        result.set("taches", objectMapper.valueToTree(todos));
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
                    if (s.getApplication() != null) {
                        node.put("application", s.getApplication().getNom());
                    }
                    return node;
                })
                .toList();
        ObjectNode result = objectMapper.createObjectNode();
        result.put("total", sessions.size());
        result.set("sessions", objectMapper.valueToTree(sessions));
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

    private String createTodo(String argsJson, UserInfo currentUser) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        String title = args.path("title").asText(null);
        if (title == null || title.isBlank()) {
            return "{\"error\": \"Le titre est obligatoire pour créer une tâche.\"}";
        }
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
        return objectMapper.writeValueAsString(result);
    }

    private String toggleTodoComplete(String argsJson, UserInfo currentUser) throws Exception {
        JsonNode args = objectMapper.readTree(argsJson);
        long id = args.path("id").asLong(0);
        if (id <= 0) {
            return "{\"error\": \"L'identifiant de la tâche (id) est obligatoire.\"}";
        }

        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new com.itaccess.exception.ResourceNotFoundException("Tâche non trouvée avec l'ID: " + id));

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
        if (title == null || title.isBlank()) {
            return "{\"error\": \"Le titre du bug est obligatoire.\"}";
        }
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
}
