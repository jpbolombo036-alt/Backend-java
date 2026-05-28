<<<<<<< HEAD
package com.itaccess.repository;

import com.itaccess.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByApplicationId(Long applicationId);
=======
package com.itaccess.repository;

import com.itaccess.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByApplicationId(Long applicationId);
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
}