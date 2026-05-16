package com.itaccess.service;

import com.itaccess.dto.HabilitationDTO;
import com.itaccess.entity.Compte;
import com.itaccess.entity.Habilitation;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.CompteRepository;
import com.itaccess.repository.HabilitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HabilitationService {
    
    private final HabilitationRepository habilitationRepository;
    private final CompteRepository compteRepository;
    
    public List<HabilitationDTO> getAllHabilitations() {
        return habilitationRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public HabilitationDTO createHabilitation(HabilitationDTO dto) {
        Compte compte = compteRepository.findById(dto.getCompteId())
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé avec l'ID: " + dto.getCompteId()));
        
        Habilitation habilitation = Habilitation.builder()
                .compteId(dto.getCompteId())
                .permission(dto.getPermission())
                .build();
        
        Habilitation savedHabilitation = habilitationRepository.save(habilitation);
        return toDTO(savedHabilitation);
    }
    
    @Transactional
    public void deleteHabilitation(Long id) {
        if (!habilitationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Habilitation non trouvée avec l'ID: " + id);
        }
        habilitationRepository.deleteById(id);
    }
    
    private HabilitationDTO toDTO(Habilitation habilitation) {
        return HabilitationDTO.builder()
                .id(habilitation.getId())
                .compteId(habilitation.getCompteId())
                .permission(habilitation.getPermission())
                .build();
    }
}