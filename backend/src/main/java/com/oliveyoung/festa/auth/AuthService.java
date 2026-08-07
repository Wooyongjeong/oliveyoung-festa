package com.oliveyoung.festa.auth;

import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthenticatedUser login(String email) {
        return userRepository.findActiveByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("등록된 활성 데모 사용자가 아닙니다."));
    }

}
