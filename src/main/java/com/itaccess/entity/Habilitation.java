package com.itaccess.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "habilitations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Habilitation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "compte_id")
    private Long compteId;
    
    @Column(nullable = false, length = 100)
    private String permission;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_id", insertable = false, updatable = false)
    private Compte compte;
}