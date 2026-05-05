package com.itaccess.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "test_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String nom;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "application_id")
    private Long applicationId;
    
    @Column(length = 50)
    private String environnement;
    
    @Column(length = 50)
    private String version;
    
    @Column(name = "nom_document", length = 200)
    private String nomDocument;
    
    @Column(name = "date_creation")
    private LocalDateTime dateCreation;
    
    @Column(length = 50)
    private String statut;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", insertable = false, updatable = false)
    private Application application;
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<Test> tests;
    
    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        if (statut == null) {
            statut = "En cours";
        }
    }
}