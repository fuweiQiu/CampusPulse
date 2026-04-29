package com.campuspulse.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "observation_record")
public class ObservationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String fhirId;

    @Column(length = 255)
    private String resourceUrl;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 64)
    private String categoryCode;

    @Column(nullable = false, length = 255)
    private String codeSystem;

    @Column(nullable = false, length = 128)
    private String codeValue;

    @Column(nullable = false, length = 255)
    private String codeDisplay;

    @Column(nullable = false)
    private LocalDateTime effectiveDateTime;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

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

    @Lob
    @Column(columnDefinition = "CLOB")
    private String suggestion;

    @Lob
    @Column(columnDefinition = "CLOB", nullable = false)
    private String resourceJson;

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

    public String getFhirId() {
        return fhirId;
    }

    public void setFhirId(String fhirId) {
        this.fhirId = fhirId;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCodeSystem() {
        return codeSystem;
    }

    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
    }

    public String getCodeValue() {
        return codeValue;
    }

    public void setCodeValue(String codeValue) {
        this.codeValue = codeValue;
    }

    public String getCodeDisplay() {
        return codeDisplay;
    }

    public void setCodeDisplay(String codeDisplay) {
        this.codeDisplay = codeDisplay;
    }

    public LocalDateTime getEffectiveDateTime() {
        return effectiveDateTime;
    }

    public void setEffectiveDateTime(LocalDateTime effectiveDateTime) {
        this.effectiveDateTime = effectiveDateTime;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
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

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getResourceJson() {
        return resourceJson;
    }

    public void setResourceJson(String resourceJson) {
        this.resourceJson = resourceJson;
    }
}
