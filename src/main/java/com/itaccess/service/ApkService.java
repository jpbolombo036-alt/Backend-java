// Déclaration du package où se trouve cette classe
package com.itaccess.service;

// Import des classes nécessaires pour le fonctionnement du service
import com.itaccess.dto.ApkFileDTO;           // DTO pour transférer les données APK
import com.itaccess.entity.ApkFile;           // Entité JPA représentant un fichier APK
import com.itaccess.exception.ResourceNotFoundException; // Exception personnalisée
import com.itaccess.config.B2Properties;      // Configuration du stockage objet B2
import com.itaccess.repository.ApkFileRepository; // Interface pour accéder à la base de données
import com.itaccess.service.B2StorageService; // Service de stockage objet (B2/S3)
import lombok.RequiredArgsConstructor;       // Annotation Lombok pour générer le constructeur
import lombok.extern.slf4j.Slf4j;           // Annotation Lombok pour les logs
import org.springframework.beans.factory.annotation.Value; // Pour injecter des valeurs depuis application.yml
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    
    // Nouveau service pour la traçabilité
    private final AuditService auditService;

    // Service de stockage objet (B2/S3) : utilisé quand activé, sinon stockage local
    private final B2StorageService b2StorageService;

    // Configuration B2 : permet de basculer local <-> stockage objet
    private final B2Properties b2Properties;

    // Préfixe des objets APK dans le bucket B2
    private static final String APK_OBJECT_PREFIX = "apk/";
    // Type MIME des APK
    private static final String APK_CONTENT_TYPE = "application/vnd.android.package-archive";
    
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

        String originalFileName = file.getOriginalFilename();

        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // Vérifie que le fichier est bien un APK (format ZIP : signature 'PK\x03\x04')
        // La validation par extension côté controller ne suffit pas ; on contrôle le contenu
        if (!isZipArchive(file.getBytes())) {
            throw new IllegalArgumentException("Le fichier n'est pas un APK valide (format attendu : archive ZIP/APK)");
        }

        // Stockage : B2 (stockage objet) si activé, sinon disque local
        String storageKey;
        if (b2Properties.isEnabled()) {
            storageKey = APK_OBJECT_PREFIX + uniqueFileName;
            log.info("Uploading APK to B2: {}", storageKey);
            b2StorageService.upload(file, storageKey, APK_CONTENT_TYPE);
            log.info("APK uploaded to B2 successfully");
        } else {
            Path uploadPath = resolveWritableUploadDirectory();
            Path filePath = uploadPath.resolve(uniqueFileName);
            log.info("Saving file to: {}", filePath.toAbsolutePath());
            try {
                Files.copy(file.getInputStream(), filePath);
                log.info("File saved successfully");
            } catch (IOException e) {
                log.error("Failed to save file: {}", e.getMessage(), e);
                throw new IOException("Impossible de sauvegarder le fichier: " + e.getMessage());
            }
            storageKey = filePath.toString();
        }

        // ÉTAPE 6 : Création de l'entité JPA pour sauvegarder en base de données
        ApkFile apkFile = ApkFile.builder()
                .fileName(uniqueFileName)           // Nom unique généré
                .originalFileName(originalFileName) // Nom original du fichier
                .filePath(storageKey)               // Clé B2 ou chemin local selon le mode de stockage
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
     * Prépare le téléchargement d'un APK : vérifie l'existence du fichier physique,
     * incrémente le compteur de téléchargements et retourne le DTO.
     * L'incrément est effectué APRÈS la vérification du fichier pour éviter
     * un effet de bord sur un GET quand le fichier est absent.
     * @param id : identifiant du fichier à télécharger
     * @return : DTO de l'APK (métadonnées mises à jour)
     */
    public ApkFileDTO downloadApk(Long id) {
        // Recherche du fichier en base de données par son ID
        ApkFile apkFile = apkFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("APK non trouvé"));

        // Vérifie d'abord l'existence du fichier physique avant toute mutation
        if (b2Properties.isEnabled()) {
            if (!b2StorageService.exists(apkFile.getFilePath())) {
                throw new ResourceNotFoundException("Fichier physique non trouvé");
            }
        } else {
            Path filePath = Paths.get(apkFile.getFilePath());
            if (!Files.exists(filePath)) {
                throw new ResourceNotFoundException("Fichier physique non trouvé");
            }
        }

        // MISE À JOUR : Incrémentation du compteur de téléchargements
        apkFile.setDownloadCount(apkFile.getDownloadCount() + 1); // Ajoute 1 au compteur actuel
        apkFileRepository.save(apkFile); // Sauvegarde la mise à jour en base

        return toDTO(apkFile);
    }

    /**
     * Construit la ressource de téléchargement à partir d'une clé de stockage.
     * @param storageKey : clé B2 (mode objet) ou chemin local (mode disque)
     * @param originalFileName : nom d'origine pour l'en-tête de téléchargement
     * @return : Ressource pour le streaming
     * @throws IOException : si le fichier ne peut être lu
     */
    public Resource loadApkResource(String storageKey, String originalFileName) throws IOException {
        if (b2Properties.isEnabled()) {
            return b2StorageService.downloadAsResource(storageKey, originalFileName, APK_CONTENT_TYPE);
        }
        Path path = Paths.get(storageKey);
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Fichier physique non trouvé");
        }
        return new UrlResource(path.toUri());
    }

    /**
     * Récupère tous les fichiers APK de la base de données (paginable).
     * @param pageable : pagination (par défaut tous les éléments, voir controller)
     * @return : liste de DTO contenant les informations des APK
     */
    public List<ApkFileDTO> getAllApks(Pageable pageable) {
        // Requête paginée, puis conversion en DTO
        return apkFileRepository.findAll(pageable).getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère tous les APK associés à une application spécifique (paginable).
     * @param applicationId : identifiant de l'application
     * @param pageable : pagination
     * @return : liste de DTO des APK de cette application
     */
    public List<ApkFileDTO> getApksByApplication(Long applicationId, Pageable pageable) {
        return apkFileRepository.findByApplicationId(applicationId, pageable).getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
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
     * Supprime un fichier APK (physiquement et en base de données).
     * Seul l'auteur de l'upload ou un administrateur peut supprimer le fichier.
     * @param id : identifiant de l'APK à supprimer
     * @param userId : ID de l'utilisateur effectuant la suppression
     * @param userRole : rôle de l'utilisateur (pour le contrôle admin)
     * @throws IOException : si erreur lors de la suppression du fichier physique
     * @throws SecurityException : si l'utilisateur n'est ni l'auteur ni admin
     */
    public void deleteApk(Long id, Long userId, String userRole) throws IOException {
        // Recherche l'APK à supprimer, lève une exception si non trouvé
        ApkFile apkFile = apkFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("APK non trouvé"));

        // Contrôle de propriété : auteur ou administrateur uniquement
        boolean isAdmin = userRole != null && "admin".equalsIgnoreCase(userRole);
        if (!isAdmin && !userId.equals(apkFile.getUploadedBy())) {
            throw new SecurityException("Vous n'êtes pas autorisé à supprimer cet APK");
        }

        // ÉTAPE 1 : Suppression du fichier physique
        if (b2Properties.isEnabled()) {
            b2StorageService.delete(apkFile.getFilePath());
        } else {
            Path filePath = Paths.get(apkFile.getFilePath()); // Convertit le chemin en objet Path
            if (Files.exists(filePath)) { // Vérifie que le fichier existe avant de le supprimer
                Files.delete(filePath); // Supprime le fichier du système de fichiers
            }
        }

        // ÉTAPE 2 : Suppression de l'entité en base de données
        apkFileRepository.delete(apkFile); // Supprime l'enregistrement de la table apk_files

        // ÉTAPE 3 : Audit de l'action
        auditService.logAction("DELETE_APK", "Fichier: " + apkFile.getOriginalFileName(), userId);
        log.info("APK deleted: {}", apkFile.getOriginalFileName());
    }

    /**
     * Vérifie qu'un fichier commence par la signature d'une archive ZIP (PK\x03\x04).
     * Les APK sont des archives ZIP, cette signature est donc obligatoire.
     * @param bytes : contenu du fichier à vérifier
     * @return : true si le fichier est une archive ZIP valide
     */
    private boolean isZipArchive(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        return bytes[0] == 0x50 // P
                && bytes[1] == 0x4B // K
                && bytes[2] == 0x03 // \x03
                && bytes[3] == 0x04; // \x04
    }
    
    private Path resolveWritableUploadDirectory() throws IOException {
        Path configuredPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            if (!Files.exists(configuredPath)) {
                Files.createDirectories(configuredPath);
            }
            if (Files.isWritable(configuredPath)) {
                return configuredPath;
            }
            log.warn("Configured upload directory is not writable, falling back to /tmp: {}", configuredPath);
        } catch (IOException e) {
            log.warn("Failed to use configured upload directory [{}], falling back to /tmp: {}", configuredPath, e.getMessage());
        }

        Path fallbackPath = Paths.get(System.getProperty("java.io.tmpdir"), "uploads", "apk").toAbsolutePath().normalize();
        if (!Files.exists(fallbackPath)) {
            Files.createDirectories(fallbackPath);
        }
        if (!Files.isWritable(fallbackPath)) {
            throw new IOException("Aucun répertoire d'upload accessible en écriture. Vérifié: " + configuredPath + " et " + fallbackPath);
        }
        return fallbackPath;
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
                 .filePath(apkFile.getFilePath())               // Chemin interne (non sérialisé)
                 .build(); // Construction finale du DTO
    }
}
