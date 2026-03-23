package com.campuspulse.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_session")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ChatSessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private PendingField pendingField;

    private Integer stressScore;

    private Double sleepHours;

    @Column(length = 64)
    private String stressAbsentReasonCode;

    @Column(length = 64)
    private String sleepAbsentReasonCode;

    @Column(length = 64)
    private String emotionAbsentReasonCode;

    @Column(length = 64)
    private String emotionCode;

    @Column(length = 128)
    private String emotionDisplay;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String sourceText;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ChatSessionStatus getStatus() {
        return status;
    }

    public void setStatus(ChatSessionStatus status) {
        this.status = status;
    }

    public PendingField getPendingField() {
        return pendingField;
    }

    public void setPendingField(PendingField pendingField) {
        this.pendingField = pendingField;
    }

    public Integer getStressScore() {
        return stressScore;
    }

    public void setStressScore(Integer stressScore) {
        this.stressScore = stressScore;
    }

    public Double getSleepHours() {
        return sleepHours;
    }

    public void setSleepHours(Double sleepHours) {
        this.sleepHours = sleepHours;
    }

    public String getStressAbsentReasonCode() {
        return stressAbsentReasonCode;
    }

    public void setStressAbsentReasonCode(String stressAbsentReasonCode) {
        this.stressAbsentReasonCode = stressAbsentReasonCode;
    }

    public String getSleepAbsentReasonCode() {
        return sleepAbsentReasonCode;
    }

    public void setSleepAbsentReasonCode(String sleepAbsentReasonCode) {
        this.sleepAbsentReasonCode = sleepAbsentReasonCode;
    }

    public String getEmotionAbsentReasonCode() {
        return emotionAbsentReasonCode;
    }

    public void setEmotionAbsentReasonCode(String emotionAbsentReasonCode) {
        this.emotionAbsentReasonCode = emotionAbsentReasonCode;
    }

    public String getEmotionCode() {
        return emotionCode;
    }

    public void setEmotionCode(String emotionCode) {
        this.emotionCode = emotionCode;
    }

    public String getEmotionDisplay() {
        return emotionDisplay;
    }

    public void setEmotionDisplay(String emotionDisplay) {
        this.emotionDisplay = emotionDisplay;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
