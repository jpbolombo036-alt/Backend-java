package com.itaccess.service;

import com.itaccess.dto.BlocNoteDTO;
import com.itaccess.dto.BlocNoteRequest;
import com.itaccess.entity.BlocNote;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.BlocNoteRepository;
import com.itaccess.security.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlocNoteService {

    private final BlocNoteRepository blocNoteRepository;

    public List<BlocNoteDTO> getAll(UserInfo currentUser) {
        if ("admin".equals(currentUser.getRole())) {
            return toDTOList(blocNoteRepository.findAll());
        }
        return toDTOList(blocNoteRepository.findByCreatedByOrderByUpdatedAtDesc(currentUser.getId()));
    }

    public BlocNoteDTO get(Long id) {
        return toDTO(findById(id));
    }

    @Transactional
    public BlocNoteDTO create(BlocNoteRequest request, UserInfo currentUser) {
        BlocNote blocNote = BlocNote.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .applicationId(request.getApplicationId())
                .sessionId(request.getSessionId())
                .testId(request.getTestId())
                .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
                .createdBy(currentUser.getId())
                .createdByUsername(currentUser.getUsername())
                .build();

        return toDTO(blocNoteRepository.save(blocNote));
    }

    @Transactional
    public BlocNoteDTO update(Long id, BlocNoteRequest request, UserInfo currentUser) {
        BlocNote blocNote = findById(id);

        if (!"admin".equals(currentUser.getRole()) && !blocNote.getCreatedBy().equals(currentUser.getId())) {
            throw new SecurityException("Accès refusé: vous n'êtes pas autorisé à modifier cette note");
        }

        blocNote.setTitle(request.getTitle());
        blocNote.setContent(request.getContent());
        blocNote.setApplicationId(request.getApplicationId());
        blocNote.setSessionId(request.getSessionId());
        blocNote.setTestId(request.getTestId());
        blocNote.setStatus(request.getStatus() != null ? request.getStatus() : blocNote.getStatus());

        return toDTO(blocNoteRepository.save(blocNote));
    }

    @Transactional
    public void delete(Long id, UserInfo currentUser) {
        BlocNote blocNote = findById(id);

        if (!"admin".equals(currentUser.getRole()) && !blocNote.getCreatedBy().equals(currentUser.getId())) {
            throw new SecurityException("Accès refusé: vous n'êtes pas autorisé à supprimer cette note");
        }

        blocNoteRepository.delete(blocNote);
    }

    private BlocNote findById(Long id) {
        return blocNoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note non trouvée avec l'ID: " + id));
    }

    private List<BlocNoteDTO> toDTOList(List<BlocNote> blocNotes) {
        return blocNotes.stream()
                .map(this::toDTO)
                .toList();
    }

    private BlocNoteDTO toDTO(BlocNote blocNote) {
        return BlocNoteDTO.builder()
                .id(blocNote.getId())
                .title(blocNote.getTitle())
                .content(blocNote.getContent())
                .applicationId(blocNote.getApplicationId())
                .sessionId(blocNote.getSessionId())
                .testId(blocNote.getTestId())
                .status(blocNote.getStatus())
                .createdBy(blocNote.getCreatedBy())
                .createdByUsername(blocNote.getCreatedByUsername())
                .createdAt(blocNote.getCreatedAt())
                .updatedAt(blocNote.getUpdatedAt())
                .build();
    }
}
