package com.oliveyoung.festa.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final HttpServletRequest request;

    public CurrentUser(HttpServletRequest request) {
        this.request = request;
    }

    public AuthenticatedUser requireAuthenticated() {
        Object user = request.getAttribute(DevAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE);
        if (user instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }
        throw new AuthenticationRequiredException();
    }

    public AuthenticatedUser requireRole(UserRole... allowedRoles) {
        AuthenticatedUser user = requireAuthenticated();
        for (UserRole allowedRole : allowedRoles) {
            if (user.role() == allowedRole) {
                return user;
            }
        }
        throw new AccessDeniedException("요청을 수행할 권한이 없습니다.");
    }
}
