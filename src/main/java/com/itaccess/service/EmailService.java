// Déclaration du package où se trouve cette classe de service email
package com.itaccess.service;

// Imports nécessaires pour le fonctionnement du service d'email
import lombok.RequiredArgsConstructor;       // Annotation Lombok pour générer le constructeur
import lombok.extern.slf4j.Slf4j;           // Annotation Lombok pour les logs
import org.springframework.beans.factory.annotation.Value; // Pour injecter des valeurs depuis application.yml
import org.springframework.mail.SimpleMailMessage; // Classe Spring pour les emails simples
import org.springframework.mail.javamail.JavaMailSender; // Interface Spring pour envoyer des emails
import org.springframework.stereotype.Service; // Annotation Spring pour marquer cette classe comme un service

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
            // ÉTAPE 1 : Création de l'email simple
            SimpleMailMessage message = new SimpleMailMessage(); // Objet Spring pour email texte
            message.setFrom(fromEmail); // Définit l'expéditeur
            message.setTo(toEmail); // Définit le destinataire
            message.setSubject("Réinitialisation de votre mot de passe - IT Access"); // Sujet de l'email
            
            // ÉTAPE 2 : Construction du lien de réinitialisation
            // Combine l'URL du frontend avec le token pour créer le lien complet
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
            
            // ÉTAPE 3 : Création du corps de l'email avec formatage
            // Utilise String.format pour insérer dynamiquement le nom et le lien
            String emailBody = String.format(
                "Bonjour %s,\n\n" + // Salutation personnalisée
                "Vous avez demandé la réinitialisation de votre mot de passe.\n\n" +
                "Cliquez sur le lien ci-dessous pour réinitialiser votre mot de passe:\n" +
                "%s\n\n" + // Lien de réinitialisation
                "Ce lien expire dans 30 minutes.\n\n" + // Information d'expiration
                "Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.\n\n" + // Sécurité
                "Cordialement,\n" +
                "L'équipe IT Access",
                username, resetLink // Paramètres pour le formatage
            );
            
            // ÉTAPE 4 : Configuration du contenu et envoi
            message.setText(emailBody); // Définit le corps de l'email
            
            mailSender.send(message); // Envoie l'email via le serveur SMTP configuré
            log.info("Email de réinitialisation envoyé avec succès à: {}", toEmail); // Confirmation de succès
        } catch (Exception e) {
            // Gestion d'erreur détaillée pour le débogage
            log.error("Erreur lors de l'envoi de l'email à {}: {}", toEmail, e.getMessage(), e);
            log.error("Type d'erreur: {}", e.getClass().getName()); // Type d'exception pour diagnostic
            // Relance une exception avec message clair pour l'appelant
            throw new RuntimeException("Impossible d'envoyer l'email de réinitialisation: " + e.getMessage(), e);
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
            // ÉTAPE 1 : Création et configuration de l'email
            SimpleMailMessage message = new SimpleMailMessage(); // Objet Spring pour email texte
            message.setFrom(fromEmail); // Définit l'expéditeur configuré
            message.setTo(toEmail); // Définit le destinataire
            message.setSubject("Vérification de votre email - IT Access"); // Sujet de l'email
            
            // ÉTAPE 2 : Construction du lien de vérification
            // Combine l'URL du frontend avec le token pour créer le lien de vérification complet
            String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;
            
            // ÉTAPE 3 : Création du corps de l'email avec formatage personnalisé
            // Utilise String.format pour insérer dynamiquement le nom et le lien de vérification
            String emailBody = String.format(
                "Bonjour %s,\n\n" + // Salutation personnalisée
                "Merci de vous être inscrit sur IT Access.\n\n" +
                "Cliquez sur le lien ci-dessous pour vérifier votre email:\n" +
                "%s\n\n" + // Lien de vérification
                "Ce lien expire dans 24 heures.\n\n" + // Information d'expiration
                "Cordialement,\n" +
                "L'équipe IT Access",
                username, verificationLink // Paramètres pour le formatage
            );
            
            // ÉTAPE 4 : Configuration du contenu et envoi de l'email
            message.setText(emailBody); // Définit le corps de l'email
            
            mailSender.send(message); // Envoie l'email via le serveur SMTP configuré
            log.info("Email de vérification envoyé à: {}", toEmail); // Log de confirmation
        } catch (Exception e) {
            // Gestion d'erreur avec logging détaillé
            log.error("Erreur lors de l'envoi de l'email à {}: {}", toEmail, e.getMessage());
            // Relance une exception avec message clair pour l'appelant
            throw new RuntimeException("Impossible d'envoyer l'email de vérification", e);
        }
    }
}
