package com.itaccess.service;

import com.itaccess.entity.Bug;
import com.itaccess.repository.BugRepository;
import com.itaccess.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BugService {
    private final BugRepository bugRepository;
    private final SystemNotificationService notificationService;

    @Transactional
    public Bug createBug(Bug bug, Long creatorId) {
        if (bug.getAssignedTo() == null) {
            bug.setAssignedTo(creatorId);
        }
        Bug saved = bugRepository.save(bug);
        
        if ("CRITICAL".equalsIgnoreCase(saved.getSeverity())) {
            notificationService.createGlobalNotification(
                "ALERTE : BUG CRITIQUE",
                "Anomalie critique détectée : " + saved.getTitle(),
                com.itaccess.entity.SystemNotification.NotificationType.ERROR,
                creatorId,
                "/bugs/" + saved.getId()
            );
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Bug> getBugsByStep(Long testStepId) {
        return bugRepository.findByTestStepId(testStepId);
    }

    @Transactional
    public Bug updateStatus(Long id, String status) {
        Bug bug = bugRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bug non trouvé : " + id));
        bug.setStatus(status);
        return bugRepository.save(bug);
    }
}