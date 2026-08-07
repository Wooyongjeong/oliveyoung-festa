package com.oliveyoung.festa.api;

import com.oliveyoung.festa.auth.AuthenticatedUser;
import com.oliveyoung.festa.auth.DevAuthenticationFilter;
import com.oliveyoung.festa.auth.LoginUserArgumentResolver;
import com.oliveyoung.festa.auth.UserRepository;
import com.oliveyoung.festa.auth.UserRole;
import com.oliveyoung.festa.order.OrderController;
import com.oliveyoung.festa.order.OrderService;
import com.oliveyoung.festa.order.OrderStatus;
import com.oliveyoung.festa.order.OrderView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({LoginUserArgumentResolver.class, WebMvcConfig.class, DevAuthenticationFilter.class})
class OrderApiTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID EVENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    OrderService orderService;

    @Test
    void createsOrderWithLoginUserAndReturnsCreatedEnvelope() throws Exception {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(customer()));
        when(orderService.createOrder(any(), eq(EVENT_ID), eq("GENERAL"), eq("order-key")))
                .thenReturn(new OrderView(UUID.randomUUID(), EVENT_ID, "테스트 이벤트", "GENERAL", "일반",
                        new BigDecimal("30000"), "KRW", OrderStatus.HELD,
                        Instant.parse("2026-08-07T01:03:00Z"), Instant.parse("2026-08-07T01:00:00Z")));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + USER_ID)
                        .header("Idempotency-Key", "order-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"" + EVENT_ID + "\",\"ticketGradeCode\":\"GENERAL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("ORDER_001"))
                .andExpect(jsonPath("$.statusMessage").value("주문 생성 성공"))
                .andExpect(jsonPath("$.body.status").value("HELD"));
    }

    private AuthenticatedUser customer() {
        return new AuthenticatedUser(USER_ID, "customer@festa.local", "데모 고객", UserRole.CUSTOMER);
    }
}
