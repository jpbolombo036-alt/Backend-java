package com.itaccess.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "comptes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compte {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "application_id")
    private Long applicationId;
    
    @Column(nullable = false, length = 100)
    private String username;
    
    @Column(columnDefinition = "TEXT")
    private String code;
    
    @Column(length = 50)
    private String role;
    
    @Column(columnDefinition = "TEXT")
    private String commentaire;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", insertable = false, updatable = false)
    private Application application;
    
    @OneToMany(mappedBy = "compte", cascade = CascadeType.ALL)
    private List<Habilitation> habilitations;
}