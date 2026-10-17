package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "subscriptions")
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subscriptionId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // AICI ESTE MODIFICAREA: nullable = true
    @ManyToOne
    @JoinColumn(name = "region_id", nullable = true)
    private Region region;

    @Column(length = 100)
    private String regionNameKeyword;

    private Boolean notifyByEmail;

    public UserSubscription() {}

    public UserSubscription(User user, Region region, String regionNameKeyword, Boolean notifyByEmail) {
        this.user = user;
        this.region = region;
        this.regionNameKeyword = regionNameKeyword;
        this.notifyByEmail = notifyByEmail;
    }

    // Getters and Setters
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public String getRegionNameKeyword() { return regionNameKeyword; }
    public void setRegionNameKeyword(String regionNameKeyword) { this.regionNameKeyword = regionNameKeyword; }
    public Boolean getNotifyByEmail() { return notifyByEmail; }
    public void setNotifyByEmail(Boolean notifyByEmail) { this.notifyByEmail = notifyByEmail; }
}