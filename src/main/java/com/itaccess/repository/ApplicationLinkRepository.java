package com.itaccess.repository;

import com.itaccess.entity.ApplicationLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationLinkRepository extends JpaRepository<ApplicationLink, Long> {
    List<ApplicationLink> findByApplicationId(Long applicationId);
}
