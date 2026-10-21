package com.avengers.evidence.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cases")
public class CaseFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String caseNumber;
    private String title;
    @Column(length = 2000)
    private String description;
    private String status = "OPEN";
    private String createdBy;
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String v) { this.caseNumber = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}
