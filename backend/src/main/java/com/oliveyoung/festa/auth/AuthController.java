package com.oliveyoung.festa.auth;

import com.oliveyoung.festa.api.ApiResponse;
import com.oliveyoung.festa.api.ApiStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/dev-login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of(ApiStatus.AUTH_LOGIN_SUCCESS, LoginResponse.from(authService.login(request.email())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@LoginUser AuthenticatedUser user) {
        return ApiResponse.of(ApiStatus.AUTH_ME_SUCCESS, UserResponse.from(user));
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
