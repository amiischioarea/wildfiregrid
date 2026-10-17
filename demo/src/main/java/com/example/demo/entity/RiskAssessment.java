package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_assessments")
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assessmentId;

    @ManyToOne
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    private Double riskScore;
    private Boolean isFireEvent;
    private LocalDateTime assessedAt;

    public RiskAssessment() {}

    public RiskAssessment(Region region, Double riskScore, Boolean isFireEvent, LocalDateTime assessedAt) {
        this.region = region;
        this.riskScore = riskScore;
        this.isFireEvent = isFireEvent;
        this.assessedAt = assessedAt;
    }

    public Long getAssessmentId() { return assessmentId; }
    public void setAssessmentId(Long assessmentId) { this.assessmentId = assessmentId; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
    public Boolean getIsFireEvent() { return isFireEvent; }
    public void setIsFireEvent(Boolean isFireEvent) { this.isFireEvent = isFireEvent; }
    public LocalDateTime getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDateTime assessedAt) { this.assessedAt = assessedAt; }
}