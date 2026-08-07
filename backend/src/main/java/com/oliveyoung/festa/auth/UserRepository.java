package com.oliveyoung.festa.auth;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {

    private final JdbcClient jdbcClient;

    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<AuthenticatedUser> findActiveByEmail(String email) {
        return jdbcClient.sql("""
                        SELECT id, email, display_name, role
                        FROM users
                        WHERE LOWER(email) = LOWER(:email) AND active = TRUE
                        """)
                .param("email", email)
                .query((rs, rowNum) -> mapUser(rs.getObject("id", UUID.class), rs.getString("email"),
                        rs.getString("display_name"), rs.getString("role")))
                .optional();
    }

    public Optional<AuthenticatedUser> findActiveById(UUID id) {
        return jdbcClient.sql("""
                        SELECT id, email, display_name, role
                        FROM users
                        WHERE id = :id AND active = TRUE
                        """)
                .param("id", id)
                .query((rs, rowNum) -> mapUser(rs.getObject("id", UUID.class), rs.getString("email"),
                        rs.getString("display_name"), rs.getString("role")))
                .optional();
    }

    private AuthenticatedUser mapUser(UUID id, String email, String displayName, String role) {
        return new AuthenticatedUser(id, email, displayName, UserRole.valueOf(role));
    }
}
