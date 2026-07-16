package com.itaccess.repository;

import com.itaccess.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByBugId(Long bugId);
    List<Attachment> findByTestStepId(Long testStepId);
    List<Attachment> findByMessageId(Long messageId);
}