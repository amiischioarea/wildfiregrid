package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_logs")
public class ApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;

    private Integer httpStatus;
    private Long responseTimeMs;
    private LocalDateTime calledAt;

    public ApiLog() {}

    public ApiLog(Region region, Integer httpStatus, Long responseTimeMs, LocalDateTime calledAt) {
        this.region = region;
        this.httpStatus = httpStatus;
        this.responseTimeMs = responseTimeMs;
        this.calledAt = calledAt;
    }

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    public LocalDateTime getCalledAt() { return calledAt; }
    public void setCalledAt(LocalDateTime calledAt) { this.calledAt = calledAt; }
}