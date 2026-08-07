package com.oliveyoung.festa.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public AuthController(UserRepository userRepository, CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @PostMapping("/dev-login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthenticatedUser user = userRepository.findActiveByEmail(request.email())
                .orElseThrow(() -> new AccessDeniedException("등록된 활성 데모 사용자가 아닙니다."));
        return LoginResponse.from(user);
    }

    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(currentUser.requireAuthenticated());
    }

    public record LoginRequest(@NotBlank @Email String email) {
    }

    public record LoginResponse(String accessToken, String tokenType, UserResponse user) {
        static LoginResponse from(AuthenticatedUser user) {
            return new LoginResponse(user.id().toString(), "Bearer", UserResponse.from(user));
        }
    }

    public record UserResponse(UUID id, String email, String displayName, UserRole role) {
        static UserResponse from(AuthenticatedUser user) {
            return new UserResponse(user.id(), user.email(), user.displayName(), user.role());
        }
    }
}
