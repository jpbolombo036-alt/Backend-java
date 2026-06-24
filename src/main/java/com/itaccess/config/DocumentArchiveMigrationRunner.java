package com.itaccess.config;

import com.itaccess.service.DocumentArchiveMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentArchiveMigrationRunner implements CommandLineRunner {

    private final DocumentArchiveMigrationService migrationService;
    private final Environment environment;

    @Override
    public void run(String... args) {
        String runMigration = environment.getProperty("RUN_DOCUMENT_ARCHIVE_MIGRATION", "false");
        if (!"true".equalsIgnoreCase(runMigration)) {
            return;
        }

        log.info("=== Lancement de la migration DocumentArchive vers B2 ===");
        try {
            var report = migrationService.migrateAll();
            log.info("=== Migration terminee: {} fichiers migres, {} fautes ===",
                    report.migrated, report.failed);
            if (!report.errors.isEmpty()) {
                report.errors.forEach(err -> log.error("Erreur migration: {}", err));
            }
        } catch (Exception e) {
            log.error("La migration a echoue", e);
        } finally {
            log.info("=== Fin de la migration DocumentArchive vers B2 ===");
        }
    }
}
