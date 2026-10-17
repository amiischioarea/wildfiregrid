package com.example.demo.repository;

import com.example.demo.entity.GridPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GridPointRepository extends JpaRepository<GridPoint, Long> {
    List<GridPoint> findByAssessment_AssessmentId(Long assessmentId);
    void deleteByAssessment_AssessmentId(Long assessmentId);
}