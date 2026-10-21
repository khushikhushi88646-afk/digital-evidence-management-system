package com.avengers.evidence.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Chain-of-custody entry. Each row stores hash(prevHash + own fields) => tamper-evident chain. */
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String timestamp;     // stored as string so hash is reproducible
    private String username;
    private String action;
    private Long evidenceId;
    @Column(length = 1000)
    private String details;
    @Column(length = 64)
    private String prevHash;
    @Column(length = 64)
    private String hash;

    public Long getId() { return id; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String v) { this.timestamp = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public Long getEvidenceId() { return evidenceId; }
    public void setEvidenceId(Long v) { this.evidenceId = v; }
    public String getDetails() { return details; }
    public void setDetails(String v) { this.details = v; }
    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String v) { this.prevHash = v; }
    public String getHash() { return hash; }
    public void setHash(String v) { this.hash = v; }
}
