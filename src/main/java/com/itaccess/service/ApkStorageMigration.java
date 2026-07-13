package com.itaccess.service;

import com.itaccess.config.B2Properties;
import com.itaccess.entity.ApkFile;
import com.itaccess.repository.ApkFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Migration one-shot (ré-exécutable sans effet) des APK stockés en local vers le stockage objet B2.
 *
 * Contexte : avant l'intégration B2, les APK étaient enregistrés sur le disque local (chemin absolu).
 * En mode B2 (app.storage.b2.enabled=true), ce composant transfère, au démarrage, les lignes dont le
 * filePath n'est pas encore une clé B2 (préfixée par "apk/") vers le bucket, puis met à jour la clé.
 * Les lignes déjà migrées (clé "apk/...") sont ignorées, donc le run suivant est un no-op.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApkStorageMigration implements CommandLineRunner {

    private static final String APK_OBJECT_PREFIX = "apk/";
    private static final String APK_CONTENT_TYPE = "application/vnd.android.package-archive";

    private final ApkFileRepository apkFileRepository;
    private final B2StorageService b2StorageService;
    private final B2Properties b2Properties;

    @Override
    public void run(String... args) {
        if (!b2Properties.isEnabled()) {
            return;
        }

        var localApks = apkFileRepository.findAll().stream()
                .filter(a -> a.getFilePath() != null && !a.getFilePath().startsWith(APK_OBJECT_PREFIX))
                .toList();

        if (localApks.isEmpty()) {
            return;
        }

        log.info("Migration APK -> B2 : {} fichier(s) local(aux) à transférer", localApks.size());
        for (ApkFile apk : localApks) {
            try {
                Path localPath = Path.of(apk.getFilePath());
                if (!Files.exists(localPath)) {
                    log.warn("Migration APK ignorée (fichier local absent) : {}", apk.getFilePath());
                    continue;
                }
                String key = APK_OBJECT_PREFIX + apk.getFileName();
                b2StorageService.upload(localPath, key, APK_CONTENT_TYPE, Files.size(localPath));
                apk.setFilePath(key);
                apkFileRepository.save(apk);
                log.info("Migration APK OK : {} -> {}", apk.getFilePath(), key);
            } catch (Exception e) {
                log.error("Migration APK échouée pour {} : {}", apk.getFilePath(), e.getMessage());
            }
        }
    }
}
