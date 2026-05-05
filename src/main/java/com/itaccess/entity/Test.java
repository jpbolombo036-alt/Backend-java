package com.itaccess.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "tests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Test {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    private TestSession session;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", insertable = false, updatable = false)
    private Application application;
}