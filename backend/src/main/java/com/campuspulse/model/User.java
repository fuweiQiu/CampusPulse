package com.campuspulse.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(length = 512)
    private String authToken;

    @Column(length = 80)
    private String displayName;

    private LocalDate birthDate;

    @Column(length = 20)
    private String gender;

    @Column(unique = true, length = 64)
    private String patientFhirId;

    @Column(length = 255)
    private String patientResourceUrl;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String patientResourceJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPatientFhirId() {
        return patientFhirId;
    }

    public void setPatientFhirId(String patientFhirId) {
        this.patientFhirId = patientFhirId;
    }

    public String getPatientResourceJson() {
        return patientResourceJson;
    }

    public void setPatientResourceJson(String patientResourceJson) {
        this.patientResourceJson = patientResourceJson;
    }

    public String getPatientResourceUrl() {
        return patientResourceUrl;
    }

    public void setPatientResourceUrl(String patientResourceUrl) {
        this.patientResourceUrl = patientResourceUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
