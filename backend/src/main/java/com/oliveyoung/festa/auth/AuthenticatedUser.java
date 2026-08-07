package com.oliveyoung.festa.auth;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, String displayName, UserRole role) {
}
