package com.oliveyoung.festa.payment;

import com.oliveyoung.festa.auth.AuthenticatedUser;
import com.oliveyoung.festa.auth.UserRole;
import com.oliveyoung.festa.order.OrderService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class PaymentIntegrationTest {
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
    static { POSTGRES.start(); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired PaymentService paymentService;
    @Autowired OrderService orderService;
    @Autowired TransactionTemplate transactions;
    @PersistenceContext EntityManager entityManager;
    @MockitoBean PaymentGateway gateway;

    private AuthenticatedUser user;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        user = new AuthenticatedUser(userId, userId + "@test.local", "결제 사용자", UserRole.CUSTOMER);
        transactions.executeWithoutResult(status -> {
            entityManager.createNativeQuery("INSERT INTO users (id,email,display_name,role) VALUES (:id,:email,'결제 사용자','CUSTOMER')")
                    .setParameter("id", userId).setParameter("email", user.email()).executeUpdate();
            entityManager.createNativeQuery("""
                    INSERT INTO events (id,name,description,sale_starts_at,sale_ends_at,event_starts_at)
                    VALUES (:id,'결제 이벤트','테스트',CURRENT_TIMESTAMP-INTERVAL '1 minute',CURRENT_TIMESTAMP+INTERVAL '1 hour',CURRENT_TIMESTAMP+INTERVAL '2 hours')
                    """).setParameter("id", eventId).executeUpdate();
            entityManager.createNativeQuery("INSERT INTO ticket_grades (id,event_id,code,name,price,currency) VALUES (:id,:eventId,'GENERAL','일반',30000,'KRW')")
                    .setParameter("id", gradeId).setParameter("eventId", eventId).executeUpdate();
            entityManager.createNativeQuery("INSERT INTO inventories (ticket_grade_id,total,available) VALUES (:id,1,1)")
                    .setParameter("id", gradeId).executeUpdate();
        });
    }

    @Test
    void approvalMovesHeldInventoryToSoldAndWritesOutbox() {
        var order = orderService.createOrder(user, eventId, "GENERAL", "order-success");
        when(gateway.approve(anyString(), any(), anyString(), any())).thenAnswer(invocation ->
                new PaymentGateway.PaymentResult(invocation.getArgument(0), new BigDecimal("30000"), "KRW",
                        PaymentGateway.Status.APPROVED, "tx-1"));

        PaymentView payment = paymentService.pay(user, order.id(), "pay-success", "SUCCESS");

        assertThat(payment.status()).isEqualTo(PaymentStatus.APPROVED);
        transactions.executeWithoutResult(status -> {
            Object[] inventory = (Object[]) entityManager.createNativeQuery("SELECT available,held,sold FROM inventories WHERE ticket_grade_id=(SELECT ticket_grade_id FROM orders WHERE id=:id)")
                    .setParameter("id", order.id()).getSingleResult();
            assertThat(((Number) inventory[1]).intValue()).isZero();
            assertThat(((Number) inventory[2]).intValue()).isEqualTo(1);
            assertThat(((Number) entityManager.createNativeQuery("SELECT count(*) FROM outbox_events WHERE aggregate_id=:id")
                    .setParameter("id", order.id()).getSingleResult()).intValue()).isEqualTo(1);
        });
    }

    @Test
    void unknownPaymentIsResolvedByGatewayLookup() {
        var order = orderService.createOrder(user, eventId, "GENERAL", "order-unknown");
        when(gateway.approve(anyString(), any(), anyString(), any())).thenThrow(new org.springframework.web.client.ResourceAccessException("timeout"));
        PaymentView unknown = paymentService.pay(user, order.id(), "pay-unknown", "TIMEOUT_AFTER_SUCCESS");
        when(gateway.find(unknown.attemptKey())).thenReturn(new PaymentGateway.PaymentResult(
                unknown.attemptKey(), new BigDecimal("30000"), "KRW", PaymentGateway.Status.APPROVED, "tx-2"));

        PaymentView reconciled = paymentService.reconcile(user, order.id());

        assertThat(unknown.status()).isEqualTo(PaymentStatus.UNKNOWN);
        assertThat(reconciled.status()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void thirdDeclineEndsOrderAndRestoresInventory() {
        var order = orderService.createOrder(user, eventId, "GENERAL", "order-decline");
        when(gateway.approve(anyString(), any(), anyString(), any())).thenAnswer(invocation ->
                new PaymentGateway.PaymentResult(invocation.getArgument(0), new BigDecimal("30000"), "KRW",
                        PaymentGateway.Status.DECLINED, null));

        for (int attempt = 1; attempt <= 3; attempt++) {
            paymentService.pay(user, order.id(), "pay-decline-" + attempt, "DECLINE");
            transactions.executeWithoutResult(status -> entityManager.createNativeQuery(
                            "UPDATE payment_attempts SET created_at=CURRENT_TIMESTAMP-INTERVAL '3 seconds' WHERE order_id=:id")
                    .setParameter("id", order.id()).executeUpdate());
        }

        transactions.executeWithoutResult(status -> {
            assertThat(entityManager.createNativeQuery("SELECT status FROM orders WHERE id=:id")
                    .setParameter("id", order.id()).getSingleResult()).isEqualTo("PAYMENT_FAILED");
            Object[] inventory = (Object[]) entityManager.createNativeQuery("SELECT available,held FROM inventories WHERE ticket_grade_id=(SELECT ticket_grade_id FROM orders WHERE id=:id)")
                    .setParameter("id", order.id()).getSingleResult();
            assertThat(((Number) inventory[0]).intValue()).isEqualTo(1);
            assertThat(((Number) inventory[1]).intValue()).isZero();
        });
    }
}
