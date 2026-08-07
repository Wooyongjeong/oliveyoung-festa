package com.oliveyoung.festa.auth;

import com.oliveyoung.festa.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseEntity {
    @Id private UUID id;
    private String email;
    @Column(name = "display_name") private String displayName;
    @Enumerated(EnumType.STRING) private UserRole role;
    private boolean active;

}
