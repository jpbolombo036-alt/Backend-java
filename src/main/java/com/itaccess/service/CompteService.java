package com.itaccess.service;

import com.itaccess.dto.CompteDTO;
import com.itaccess.dto.CompteRequest;
import com.itaccess.dto.PageResponse;
import com.itaccess.entity.Application;
import com.itaccess.entity.Compte;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.ApplicationRepository;
import com.itaccess.repository.CompteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompteService {
    
    private final CompteRepository compteRepository;
    private final ApplicationRepository applicationRepository;
    
    public PageResponse<CompteDTO> getAllComptes(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Compte> comptePage = compteRepository.findAll(pageable);
        
        List<CompteDTO> content = comptePage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return PageResponse.of(content, comptePage.getNumber(), comptePage.getSize(), comptePage.getTotalElements());
    }
    
    public CompteDTO getCompteById(Long id) {
        Compte compte = compteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé avec l'ID: " + id));
        return toDTO(compte);
    }
    
    @Transactional
    public CompteDTO createCompte(CompteRequest request, Long createdBy) {
        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application non trouvée avec l'ID: " + request.getApplicationId()));
        
        Compte compte = Compte.builder()
                .applicationId(request.getApplicationId())
                .username(request.getUsername())
                .code(request.getCode())
                .role(request.getRole())
                .commentaire(request.getCommentaire())
                .createdBy(createdBy)
                .build();
        
        Compte savedCompte = compteRepository.save(compte);
        return toDTO(savedCompte);
    }
    
    @Transactional
    public CompteDTO updateCompte(Long id, CompteRequest request, Long userId, String userRole) {
        Compte compte = compteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé avec l'ID: " + id));
        
        if (!"admin".equals(userRole) && !compte.getCreatedBy().equals(userId)) {
            throw new SecurityException("Non autorisé à mettre à jour ce compte");
        }
        
        compte.setApplicationId(request.getApplicationId());
        compte.setUsername(request.getUsername());
        if (request.getCode() != null) {
            compte.setCode(request.getCode());
        }
        compte.setRole(request.getRole());
        compte.setCommentaire(request.getCommentaire());
        
        Compte updatedCompte = compteRepository.save(compte);
        return toDTO(updatedCompte);
    }
    
    @Transactional
    public void deleteCompte(Long id, Long userId, String userRole) {
        Compte compte = compteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé avec l'ID: " + id));
        
        if (!"admin".equals(userRole) && !compte.getCreatedBy().equals(userId)) {
            throw new SecurityException("Non autorisé à supprimer ce compte");
        }
        
        compteRepository.deleteById(id);
    }
    
    private CompteDTO toDTO(Compte compte) {
        CompteDTO.CompteDTOBuilder builder = CompteDTO.builder()
                .id(compte.getId())
                .applicationId(compte.getApplicationId())
                .username(compte.getUsername())
                .code(compte.getCode())
                .role(compte.getRole())
                .commentaire(compte.getCommentaire())
                .createdBy(compte.getCreatedBy());
        
        if (compte.getApplication() != null) {
            builder.application(CompteDTO.ApplicationInfoDTO.builder()
                    .id(compte.getApplication().getId())
                    .nom(compte.getApplication().getNom())
                    .build());
        }
        
        return builder.build();
    }
}