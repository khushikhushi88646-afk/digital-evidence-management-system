package com.avengers.evidence.model;

import jakarta.persistence.*;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(nullable = false)
    private String passwordHash;
    private String fullName;
    @Enumerated(EnumType.STRING)
    private Role role;

    public AppUser() {}
    public AppUser(String username, String passwordHash, String fullName, Role role) {
        this.username = username; this.passwordHash = passwordHash; this.fullName = fullName; this.role = role;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
}
