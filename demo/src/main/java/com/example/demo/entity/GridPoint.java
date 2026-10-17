package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "grid_points")
public class GridPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pointId;

    @ManyToOne
    @JoinColumn(name = "assessment_id", nullable = false)
    private RiskAssessment assessment;

    private Double latitude;
    private Double longitude;
    private Double localRiskScore;

    public GridPoint() {}

    public GridPoint(RiskAssessment assessment, Double latitude, Double longitude, Double localRiskScore) {
        this.assessment = assessment;
        this.latitude = latitude;
        this.longitude = longitude;
        this.localRiskScore = localRiskScore;
    }

    public Long getPointId() { return pointId; }
    public void setPointId(Long pointId) { this.pointId = pointId; }
    public RiskAssessment getAssessment() { return assessment; }
    public void setAssessment(RiskAssessment assessment) { this.assessment = assessment; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getLocalRiskScore() { return localRiskScore; }
    public void setLocalRiskScore(Double localRiskScore) { this.localRiskScore = localRiskScore; }
}