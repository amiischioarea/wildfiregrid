package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "regions")
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long regionId;

    @Column(name = "region_name", nullable = false)
    private String regionName;

    @Column(name = "center_latitude", nullable = false)
    private Double centerLatitude;

    @Column(name = "center_longitude", nullable = false)
    private Double centerLongitude;

    @Column(name = "country")
    private String country;

    public Region() {}

    public Region(String regionName, Double centerLatitude, Double centerLongitude, String country) {
        this.regionName = regionName;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.country = country;
    }

    public Long getRegionId() { return regionId; }
    public void setRegionId(Long regionId) { this.regionId = regionId; }
    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }
    public Double getCenterLatitude() { return centerLatitude; }
    public void setCenterLatitude(Double centerLatitude) { this.centerLatitude = centerLatitude; }
    public Double getCenterLongitude() { return centerLongitude; }
    public void setCenterLongitude(Double centerLongitude) { this.centerLongitude = centerLongitude; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}