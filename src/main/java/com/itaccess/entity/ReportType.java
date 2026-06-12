package com.itaccess.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum ReportType {
    SECURITY("security", "Rapport de sécurité", "Analyse complète des accès et des droits utilisateurs."),
    ACCESS("access", "Journal des accès", "Historique détaillé de toutes les connexions."),
    TESTS("tests", "Rapport de tests", "Synthèse des campagnes de test et résultats."),
    PERFORMANCE("performance", "Performance globale", "Statistiques d'utilisation et métriques clés."),
    COMPLIANCE("compliance", "Conformité", "Audit des accès selon les politiques de sécurité.");

    private final String id;
    private final String title;
    private final String description;

    public static Optional<ReportType> fromId(String id) {
        return Arrays.stream(values())
                .filter(type -> type.id.equalsIgnoreCase(id))
                .findFirst();
    }
}
