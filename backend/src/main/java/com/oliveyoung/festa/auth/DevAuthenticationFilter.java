package com.oliveyoung.festa.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class DevAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHENTICATED_USER_ATTRIBUTE = DevAuthenticationFilter.class.getName() + ".user";
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;

    public DevAuthenticationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            try {
                UUID userId = UUID.fromString(authorization.substring(BEARER_PREFIX.length()));
                userRepository.findActiveById(userId)
                        .ifPresent(user -> request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, user));
            } catch (IllegalArgumentException ignored) {
                // Invalid development tokens are treated as unauthenticated.
            }
        }
        filterChain.doFilter(request, response);
    }
}
