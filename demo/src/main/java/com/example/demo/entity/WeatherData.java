package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_data")
public class WeatherData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long weatherId;

    @ManyToOne
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    private Double temperatureCelsius;
    private Integer humidityPercent;
    private Double windSpeedKmh;
    private Integer windDirectionDeg;
    private LocalDateTime recordedAt;

    public WeatherData() {}

    public WeatherData(Region region, Double temperatureCelsius, Integer humidityPercent, Double windSpeedKmh, Integer windDirectionDeg, LocalDateTime recordedAt) {
        this.region = region;
        this.temperatureCelsius = temperatureCelsius;
        this.humidityPercent = humidityPercent;
        this.windSpeedKmh = windSpeedKmh;
        this.windDirectionDeg = windDirectionDeg;
        this.recordedAt = recordedAt;
    }

    public Long getWeatherId() { return weatherId; }
    public void setWeatherId(Long weatherId) { this.weatherId = weatherId; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public Double getTemperatureCelsius() { return temperatureCelsius; }
    public void setTemperatureCelsius(Double temperatureCelsius) { this.temperatureCelsius = temperatureCelsius; }
    public Integer getHumidityPercent() { return humidityPercent; }
    public void setHumidityPercent(Integer humidityPercent) { this.humidityPercent = humidityPercent; }
    public Double getWindSpeedKmh() { return windSpeedKmh; }
    public void setWindSpeedKmh(Double windSpeedKmh) { this.windSpeedKmh = windSpeedKmh; }
    public Integer getWindDirectionDeg() { return windDirectionDeg; }
    public void setWindDirectionDeg(Integer windDirectionDeg) { this.windDirectionDeg = windDirectionDeg; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}