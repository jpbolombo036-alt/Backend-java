package com.itaccess.repository;

import com.itaccess.entity.DocumentArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentArchiveRepository extends JpaRepository<DocumentArchive, Long> {

}
