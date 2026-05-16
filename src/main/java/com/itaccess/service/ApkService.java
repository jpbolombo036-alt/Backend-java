// Déclaration du package où se trouve cette classe
package com.itaccess.service;

// Import des classes nécessaires pour le fonctionnement du service
import com.itaccess.dto.ApkFileDTO;           // DTO pour transférer les données APK
import com.itaccess.entity.ApkFile;           // Entité JPA représentant un fichier APK
import com.itaccess.exception.ResourceNotFoundException; // Exception personnalisée
import com.itaccess.repository.ApkFileRepository; // Interface pour accéder à la base de données
import lombok.RequiredArgsConstructor;       // Annotation Lombok pour générer le constructeur
import lombok.extern.slf4j.Slf4j;           // Annotation Lombok pour les logs
import org.springframework.beans.factory.annotation.Value; // Pour injecter des valeurs depuis application.yml
import org.springframework.stereotype.Service; // Annotation Spring pour marquer cette classe comme un service
import org.springframework.web.multipart.MultipartFile; // Pour gérer les fichiers uploadés

// Imports Java pour la gestion des fichiers et collections
import java.io.IOException;                  // Pour gérer les erreurs d'entrée/sortie
import java.nio.file.Files;                  // Pour manipuler les fichiers
import java.nio.file.Path;                   // Pour représenter un chemin de fichier
import java.nio.file.Paths;                  // Pour créer des chemins
import java.util.List;                       // Pour les listes
import java.util.UUID;                        // Pour générer des identifiants uniques
import java.util.stream.Collectors;          // Pour les opérations sur les streams

// Annotation Spring : cette classe est un service métier
@Service
// Génère automatiquement un constructeur avec tous les champs finaux
@RequiredArgsConstructor
// Génère automatiquement un logger pour cette classe
@Slf4j
public class ApkService {
    
    // Repository pour interagir avec la table apk_files en base de données
    // 'final' car injecté par Spring et ne doit pas changer
    private final ApkFileRepository apkFileRepository;
    
    // Injecte la valeur depuis application.yml (clé app.upload.dir)
    // Valeur par défaut : "uploads/apk" si non définie dans le fichier de config
    @Value("${app.upload.dir:uploads/apk}")
    private String uploadDir;
    
    /**
     * Méthode principale pour uploader un fichier APK
     * @param file : le fichier multipart reçu du client
     * @param applicationId : ID optionnel de l'application associée
     * @param uploadedBy : ID de l'utilisateur qui upload le fichier
     * @param version : version optionnelle de l'APK
     * @param packageName : nom du package optionnel
     * @param description : description optionnelle
     * @return : DTO avec les informations du fichier sauvegardé
     * @throws IOException : en cas d'erreur de manipulation de fichiers
     */
    public ApkFileDTO uploadApk(MultipartFile file, Long applicationId, Long uploadedBy, 
                                  String version, String packageName, String description) throws IOException {
        // Log d'information pour tracer le début de l'upload
        log.info("Starting APK upload: file={}, size={}, user={}", file.getOriginalFilename(), file.getSize(), uploadedBy);
        
        // ÉTAPE 1 : Préparation du répertoire d'upload
        // Crée un objet Path à partir du chemin configuré, le rend absolu et normalisé
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        log.info("Upload directory: {}", uploadPath); // Log pour debug du chemin utilisé
        
        // ÉTAPE 2 : Vérification et création du répertoire si nécessaire
        if (!Files.exists(uploadPath)) { // Si le répertoire n'existe pas encore
            log.info("Directory does not exist, creating: {}", uploadPath);
            try {
                Files.createDirectories(uploadPath); // Crée le répertoire et tous les parents nécessaires
                log.info("Directory created successfully"); // Confirmation de création
            } catch (IOException e) {
                log.error("Failed to create upload directory: {}", e.getMessage(), e);
                throw new IOException("Impossible de créer le répertoire d'upload: " + e.getMessage());
            }
        }
        
        // ÉTAPE 3 : Vérification des permissions d'écriture du répertoire
        if (!Files.isWritable(uploadPath)) { // Vérifie si on peut écrire dans ce répertoire
            log.error("Upload directory is not writable: {}", uploadPath);
            throw new IOException("Le répertoire d'upload n'est pas accessible en écriture: " + uploadPath);
        }
        
        // ÉTAPE 4 : Génération d'un nom de fichier unique pour éviter les conflits
        String originalFileName = file.getOriginalFilename(); // Récupère le nom original du fichier
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new IOException("Nom de fichier invalide"); // Validation du nom de fichier
        }
        
        // Extraction de l'extension du fichier original (ex: ".apk")
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        // Génération d'un nom unique avec UUID + extension originale
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        // Construction du chemin complet du fichier dans le répertoire d'upload
        Path filePath = uploadPath.resolve(uniqueFileName);
        
        // Log pour tracer où le fichier sera sauvegardé
        log.info("Saving file to: {}", filePath.toAbsolutePath());
        
        // ÉTAPE 5 : Sauvegarde physique du fichier sur le disque
        try {
            // Copie le flux d'entrée du fichier uploadé vers le chemin de destination
            Files.copy(file.getInputStream(), filePath);
            log.info("File saved successfully"); // Confirmation de sauvegarde
        } catch (IOException e) {
            log.error("Failed to save file: {}", e.getMessage(), e);
            throw new IOException("Impossible de sauvegarder le fichier: " + e.getMessage());
        }
        
        // ÉTAPE 6 : Création de l'entité JPA pour sauvegarder en base de données
        ApkFile apkFile = ApkFile.builder()
                .fileName(uniqueFileName)           // Nom unique généré
                .originalFileName(originalFileName) // Nom original du fichier
                .filePath(filePath.toString())      // Chemin complet du fichier
                .fileSize(file.getSize())           // Taille en octets
                .version(version)                   // Version de l'APK (optionnel)
                .packageName(packageName)           // Nom du package (optionnel)
                .description(description)           // Description (optionnel)
                .applicationId(applicationId)       // ID de l'application associée
                .uploadedBy(uploadedBy)            // ID de l'utilisateur qui a uploadé
                .build(); // Construction finale de l'objet
        
        // ÉTAPE 7 : Sauvegarde en base de données
        ApkFile saved = apkFileRepository.save(apkFile); // Insère l'entité dans la table apk_files
        log.info("APK uploaded successfully: {} by user {}", originalFileName, uploadedBy);
        
        // ÉTAPE 8 : Conversion et retour du DTO
        return toDTO(saved); // Transforme l'entité en DTO pour le retour au client
    }
    
    /**
     * Méthode pour télécharger un fichier APK
     * @param id : identifiant du fichier à télécharger
     * @return : tableau d'octets représentant le fichier
     * @throws IOException : si le fichier ne peut être lu
     */
    public byte[] downloadApk(Long id) throws IOException {
        // Recherche du fichier en base de données par son ID
        ApkFile apkFile = apkFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("APK non trouvé"));
        
        // MISE À JOUR : Incrémentation du compteur de téléchargements
        apkFile.setDownloadCount(apkFile.getDownloadCount() + 1); // Ajoute 1 au compteur actuel
        apkFileRepository.save(apkFile); // Sauvegarde la mise à jour en base
        
        // LECTURE : Lecture du fichier physique depuis le disque
        Path filePath = Paths.get(apkFile.getFilePath()); // Convertit le chemin string en objet Path
        return Files.readAllBytes(filePath); // Lit tout le fichier et retourne les octets
    }
    
    /**
     * Récupère tous les fichiers APK de la base de données
     * @return : liste de DTO contenant les informations de tous les APK
     */
    public List<ApkFileDTO> getAllApks() {
        // Requête pour trouver tous les APK, puis conversion en stream pour traitement
        return apkFileRepository.findAll().stream() // Récupère tous les enregistrements
                .map(this::toDTO)                    // Convertit chaque entité en DTO
                .collect(Collectors.toList());       // Collecte les résultats dans une liste
    }
    
    /**
     * Récupère tous les APK associés à une application spécifique
     * @param applicationId : identifiant de l'application
     * @return : liste de DTO des APK de cette application
     */
    public List<ApkFileDTO> getApksByApplication(Long applicationId) {
        // Utilise la méthode custom du repository pour filtrer par applicationId
        return apkFileRepository.findByApplicationId(applicationId).stream()
                .map(this::toDTO)                    // Conversion en DTO
                .collect(Collectors.toList());       // Collecte en liste
    }
    
    /**
     * Récupère un APK spécifique par son identifiant
     * @param id : identifiant de l'APK recherché
     * @return : DTO contenant les informations de l'APK
     */
    public ApkFileDTO getApkById(Long id) {
        // Recherche l'APK par ID, lève une exception si non trouvé
        ApkFile apkFile = apkFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("APK non trouvé"));
        return toDTO(apkFile); // Conversion en DTO pour le retour
    }
    
    /**
     * Supprime un fichier APK (physiquement et en base de données)
     * @param id : identifiant de l'APK à supprimer
     * @throws IOException : si erreur lors de la suppression du fichier physique
     */
    public void deleteApk(Long id) throws IOException {
        // Recherche l'APK à supprimer, lève une exception si non trouvé
        ApkFile apkFile = apkFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("APK non trouvé"));
        
        // ÉTAPE 1 : Suppression du fichier physique du disque
        Path filePath = Paths.get(apkFile.getFilePath()); // Convertit le chemin en objet Path
        if (Files.exists(filePath)) { // Vérifie que le fichier existe avant de le supprimer
            Files.delete(filePath); // Supprime le fichier du système de fichiers
        }
        
        // ÉTAPE 2 : Suppression de l'entité en base de données
        apkFileRepository.delete(apkFile); // Supprime l'enregistrement de la table apk_files
        log.info("APK deleted: {}", apkFile.getOriginalFileName()); // Log de confirmation
    }
    
    /**
     * Méthode utilitaire privée pour convertir une entité ApkFile en DTO
     * @param apkFile : entité à convertir
     * @return : DTO correspondant avec les mêmes données
     */
    private ApkFileDTO toDTO(ApkFile apkFile) {
        // Utilise le pattern Builder pour créer le DTO avec toutes les propriétés
        return ApkFileDTO.builder()
                .id(apkFile.getId())                           // ID de l'entité
                .fileName(apkFile.getFileName())               // Nom unique du fichier
                .originalFileName(apkFile.getOriginalFileName()) // Nom original
                .fileSize(apkFile.getFileSize())               // Taille en octets
                .version(apkFile.getVersion())                 // Version de l'APK
                .packageName(apkFile.getPackageName())         // Nom du package
                .description(apkFile.getDescription())         // Description
                .applicationId(apkFile.getApplicationId())     // ID application associée
                .uploadedBy(apkFile.getUploadedBy())            // ID utilisateur qui a uploadé
                .uploadDate(apkFile.getUploadDate())           // Date d'upload
                .downloadCount(apkFile.getDownloadCount())     // Nombre de téléchargements
                .build(); // Construction finale du DTO
    }
}
