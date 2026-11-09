package com.example.demo.repository;

import com.example.demo.entity.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {

    @Query("SELECT r FROM RiskAssessment r WHERE r.riskScore >= :min AND r.riskScore < :max ORDER BY r.assessedAt DESC")
    List<RiskAssessment> findLatestInRange(@Param("min") double min, @Param("max") double max);

}