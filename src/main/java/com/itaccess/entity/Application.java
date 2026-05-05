package com.itaccess.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String nom;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 50)
    private String version;
    
    @Column(length = 50)
    private String environnement;
    
    @Column(name = "date_creation")
    private LocalDateTime dateCreation;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    private List<Compte> comptes;
    
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    private List<Test> tests;
    
    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }
}