package com.avengers.evidence.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "evidence")
public class Evidence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long caseId;
    private String title;
    @Column(length = 2000)
    private String description;
    private String evidenceType;     // IMAGE, VIDEO, DOCUMENT, CHAT_LOG, OTHER
    private String originalFileName;
    private String storedFileName;
    private String contentType;
    private long sizeBytes;
    @Column(length = 64)
    private String sha256;
    private String integrityStatus = "UNVERIFIED"; // UNVERIFIED | VERIFIED | TAMPERED
    private String uploadedBy;
    private Instant uploadedAt = Instant.now();
    private Instant lastVerifiedAt;

    public Long getId() { return id; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long v) { this.caseId = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String v) { this.evidenceType = v; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String v) { this.originalFileName = v; }
    public String getStoredFileName() { return storedFileName; }
    public void setStoredFileName(String v) { this.storedFileName = v; }
    public String getContentType() { return contentType; }
    public void setContentType(String v) { this.contentType = v; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long v) { this.sizeBytes = v; }
    public String getSha256() { return sha256; }
    public void setSha256(String v) { this.sha256 = v; }
    public String getIntegrityStatus() { return integrityStatus; }
    public void setIntegrityStatus(String v) { this.integrityStatus = v; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String v) { this.uploadedBy = v; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant v) { this.uploadedAt = v; }
    public Instant getLastVerifiedAt() { return lastVerifiedAt; }
    public void setLastVerifiedAt(Instant v) { this.lastVerifiedAt = v; }
}
