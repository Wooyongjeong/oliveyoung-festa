package com.oliveyoung.festa.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.email) = LOWER(:email) AND u.active = true")
    Optional<UserEntity> findActiveEntityByEmail(String email);

    Optional<UserEntity> findByIdAndActiveTrue(UUID id);

    default Optional<AuthenticatedUser> findActiveByEmail(String email) {
        return findActiveEntityByEmail(email).map(this::toAuthenticatedUser);
    }

    default Optional<AuthenticatedUser> findActiveById(UUID id) {
        return findByIdAndActiveTrue(id).map(this::toAuthenticatedUser);
    }

    private AuthenticatedUser toAuthenticatedUser(UserEntity user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }
}
