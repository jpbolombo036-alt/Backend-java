// Déclaration du package où se trouve cette classe de service email
package com.itaccess.service;

// Imports nécessaires pour le fonctionnement du service d'email
import lombok.RequiredArgsConstructor;       // Annotation Lombok pour générer le constructeur
import org.springframework.beans.factory.annotation.Value; // Pour injecter des valeurs depuis application.yml
import org.springframework.mail.SimpleMailMessage; // Classe Spring pour les emails simples
import org.springframework.mail.javamail.JavaMailSender; // Interface Spring pour envoyer des emails
import org.springframework.mail.javamail.MimeMessageHelper; // Pour les emails HTML
import org.springframework.stereotype.Service; // Annotation Spring pour marquer cette classe comme un service
import org.thymeleaf.TemplateEngine; // Moteur de template Thymeleaf
import org.thymeleaf.context.Context; // Contexte pour les templates Thymeleaf
import lombok.extern.slf4j.Slf4j;           // Annotation Lombok pour les logs
import jakarta.mail.internet.MimeMessage; // Pour les emails MIME (HTML)

// Annotation Spring : cette classe est un service métier pour la gestion des emails
@Service
// Génère automatiquement un constructeur avec tous les champs finaux
@RequiredArgsConstructor
// Génère automatiquement un logger pour cette classe
@Slf4j
public class EmailService {
    
    // Injecté par Spring : composant principal pour envoyer des emails
    // 'final' car injecté par Spring et ne doit pas changer
    private final JavaMailSender mailSender;
    
    // Injecte l'adresse email d'expéditeur depuis application.yml
    // Valeur par défaut : "noreply@itaccess.local" si non définie
    @Value("${spring.mail.from:noreply@itaccess.local}")
    private String fromEmail;
    
    // Injecte l'URL du frontend depuis application.yml pour construire les liens
    // Valeur par défaut : "http://localhost:3000" si non définie
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // Injecte le moteur de template Thymeleaf
    private final TemplateEngine templateEngine;
    
    /**
     * Envoie un email de réinitialisation de mot de passe
     * @param toEmail : adresse email du destinataire
     * @param username : nom de l'utilisateur pour personnaliser l'email
     * @param resetToken : token unique pour la réinitialisation
     */
    public void sendPasswordResetEmail(String toEmail, String username, String resetToken) {
        // Logs pour tracer la tentative d'envoi et les configurations utilisées
        log.info("Tentative d'envoi d'email de réinitialisation à: {}", toEmail);
        log.info("From email: {}", fromEmail);
        log.info("Frontend URL: {}", frontendUrl);
        
        try {
            // ÉTAPE 1 : Construction du lien de réinitialisation
            // Combine l'URL du frontend avec le token pour créer le lien complet
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
            
            // ÉTAPE 2 : Préparation du contexte Thymeleaf
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("resetLink", resetLink);
            context.setVariable("expirationMinutes", 30); // Ou une variable dynamique si l'expiration est configurable

            // ÉTAPE 3 : Traitement du template HTML
            String htmlContent = templateEngine.process("password-reset-email", context);

            // ÉTAPE 4 : Envoi de l'email HTML
            sendHtmlEmail(toEmail, "Réinitialisation de votre mot de passe - IT Access", htmlContent);

            log.info("Email de réinitialisation envoyé avec succès à: {}", toEmail); // Confirmation de succès
        } catch (Exception e) {
            // Gestion d'erreur détaillée pour le débogage
            log.error("Erreur lors de l'envoi de l'email à {}: {}", toEmail, e.getMessage(), e);
            // Relance une exception avec message clair pour l'appelant
            throw new RuntimeException("Échec de l'envoi de l'email de réinitialisation", e);
        }
    }
    
    /**
     * Envoie un email de vérification d'adresse email
     * @param toEmail : adresse email à vérifier
     * @param username : nom de l'utilisateur pour personnaliser l'email
     * @param verificationToken : token unique pour la vérification
     */
    public void sendEmailVerification(String toEmail, String username, String verificationToken) {
        try {
            // ÉTAPE 1 : Construction du lien de vérification
            // Combine l'URL du frontend avec le token pour créer le lien de vérification complet
            String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;
            
            // ÉTAPE 2 : Préparation du contexte Thymeleaf
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("verificationLink", verificationLink);
            context.setVariable("expirationHours", 24); // Ou une variable dynamique

            // ÉTAPE 3 : Traitement du template HTML
            String htmlContent = templateEngine.process("email-verification", context);

            // ÉTAPE 4 : Envoi de l'email HTML
            sendHtmlEmail(toEmail, "Vérification de votre email - IT Access", htmlContent);

            log.info("Email de vérification envoyé à: {}", toEmail); // Log de confirmation
        } catch (Exception e) {
            // Gestion d'erreur avec logging détaillé
            log.error("Erreur lors de l'envoi de l'email à {}: {}", toEmail, e.getMessage());
            // On propage l'exception originale pour ne pas perdre la cause
            throw new RuntimeException("Échec de l'envoi de l'email de vérification", e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        try {
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true indique que le contenu est HTML
            mailSender.send(mimeMessage);
        } catch (jakarta.mail.MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email HTML à {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email HTML", e);
        }
    }
}
