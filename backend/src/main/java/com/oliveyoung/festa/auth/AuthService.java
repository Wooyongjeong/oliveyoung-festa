package com.oliveyoung.festa.auth;

import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public AuthService(UserRepository userRepository, CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    public AuthenticatedUser login(String email) {
        return userRepository.findActiveByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("등록된 활성 데모 사용자가 아닙니다."));
    }

    public AuthenticatedUser getCurrentUser() {
        return currentUser.requireAuthenticated();
    }
}
