package com.itaccess.entity;

import jakarta.persistence.*;
import lombok.*; // Utilisez le même package lombok pour la nouvelle entité
import java.util.List;

@Entity
@Table(name = "test_steps") // Renommer la table en 'test_steps'
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_case_id") // Nouvelle clé étrangère vers le TestCase
    private Long testCaseId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "application_nom", length = 100)
    private String applicationNom;

    @Column(length = 50)
    private String version;

    @Column(length = 50)
    private String environnement;

    @Column(nullable = false, length = 200)
    private String fonction;

    @Column(columnDefinition = "TEXT")
    private String precondition;

    @Column(columnDefinition = "TEXT")
    private String etapes;

    @Column(name = "resultat_attendu", columnDefinition = "TEXT")
    private String resultatAttendu;

    @Column(name = "resultat_obtenu", columnDefinition = "TEXT")
    private String resultatObtenu;

    @Column(length = 50)
    private String statut;

    @Column(columnDefinition = "TEXT")
    private String commentaires;

    @Column(name = "created_by")
    private Long createdBy;

    // Nouveau champ pour le numéro de test séquentiel par session
    @Column(name = "test_number")
    private Long testNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    private TestSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", insertable = false, updatable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY) // Relation avec la nouvelle entité TestCase
    @JoinColumn(name = "test_case_id", insertable = false, updatable = false)
    private TestCase testCase;

    @OneToMany(mappedBy = "testStepId", cascade = CascadeType.ALL)
    private List<Attachment> attachments;
}