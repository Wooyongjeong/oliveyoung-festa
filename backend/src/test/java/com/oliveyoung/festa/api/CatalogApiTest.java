package com.oliveyoung.festa.api;

import com.oliveyoung.festa.auth.AuthController;
import com.oliveyoung.festa.auth.AuthenticatedUser;
import com.oliveyoung.festa.auth.CurrentUser;
import com.oliveyoung.festa.auth.DevAuthenticationFilter;
import com.oliveyoung.festa.auth.UserRepository;
import com.oliveyoung.festa.auth.UserRole;
import com.oliveyoung.festa.catalog.EventController;
import com.oliveyoung.festa.catalog.EventRepository;
import com.oliveyoung.festa.catalog.EventSaleStatus;
import com.oliveyoung.festa.catalog.EventSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, EventController.class})
@Import({CurrentUser.class, DevAuthenticationFilter.class})
class CatalogApiTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    EventRepository eventRepository;

    @Test
    void developmentLoginReturnsBearerTokenAndUserRole() throws Exception {
        AuthenticatedUser user = customer();
        when(userRepository.findActiveByEmail("customer@festa.local")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"customer@festa.local\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(USER_ID.toString()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    void eventsRequireAuthenticationAndUseStandardError() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void authenticatedUserCanListEvents() throws Exception {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(customer()));
        when(eventRepository.findAll()).thenReturn(List.of(new EventSummary(
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                "올리브영 페스타 2026",
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z"),
                EventSaleStatus.ON_SALE)));

        mockMvc.perform(get("/api/events")
                        .header("Authorization", "Bearer " + USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("올리브영 페스타 2026"))
                .andExpect(jsonPath("$[0].status").value("ON_SALE"));
    }

    @Test
    void unknownDevelopmentUserGetsStandardForbiddenError() throws Exception {
        when(userRepository.findActiveByEmail("unknown@festa.local")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@festa.local\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    private AuthenticatedUser customer() {
        return new AuthenticatedUser(USER_ID, "customer@festa.local", "데모 고객", UserRole.CUSTOMER);
    }
}
