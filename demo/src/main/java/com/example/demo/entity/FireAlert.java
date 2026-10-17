package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fire_alerts")
public class FireAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alertId;

    @ManyToOne
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    private String severityLevel;
    private String message;
    private LocalDateTime issuedAt;

    public FireAlert() {}

    public FireAlert(Region region, String severityLevel, String message, LocalDateTime issuedAt) {
        this.region = region;
        this.severityLevel = severityLevel;
        this.message = message;
        this.issuedAt = issuedAt;
    }

    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public String getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(String severityLevel) { this.severityLevel = severityLevel; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
}