package com.oliveyoung.festa.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id private UUID id;
    private String email;
    @Column(name = "display_name") private String displayName;
    @Enumerated(EnumType.STRING) private UserRole role;
    private boolean active;

    protected UserEntity() {}
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return active; }
}
