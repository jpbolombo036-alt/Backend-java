package com.itaccess.repository;

import com.itaccess.entity.BlocNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlocNoteRepository extends JpaRepository<BlocNote, Long> {

    List<BlocNote> findByCreatedByOrderByUpdatedAtDesc(Long createdBy);

    List<BlocNote> findByApplicationIdOrderByUpdatedAtDesc(Long applicationId);

    List<BlocNote> findBySessionIdOrderByUpdatedAtDesc(Long sessionId);

    List<BlocNote> findByTestIdOrderByUpdatedAtDesc(Long testId);
}
