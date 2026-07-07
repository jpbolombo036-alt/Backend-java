package com.itaccess.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itaccess.dto.AiChatRequest;
import com.itaccess.dto.AiChatResponse;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.*;
import com.itaccess.security.UserInfo;
import com.itaccess.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
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
    private final MessageRepository messageRepository;
    private final SystemNotificationRepository systemNotificationRepository;
    private final ReportGenerationRepository reportGenerationRepository;
    private final HabilitationRepository habilitationRepository;
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

    public AiChatResponse chat(List<AiChatRequest.AiMessage> messages, UserInfo currentUser) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return AiChatResponse.builder()
                    .error(true)
                    .errorMessage("Clé API OpenAI non configurée. Ajoutez OPENAI_API_KEY dans votre fichier .env")
                    .build();
        }

        try {
            RestClient restClient = RestClient.create();
            ObjectNode requestBody = buildRequestBody(messages, currentUser);

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

    private ObjectNode buildRequestBody(List<AiChatRequest.AiMessage> messages, UserInfo currentUser) {
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

        body.set("tools", buildTools());
        body.put("tool_choice", "auto");

        return body;
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
        var notifs = systemNotificationRepository.findAllByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
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

        var notifications = systemNotificationRepository.findAllByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
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
}