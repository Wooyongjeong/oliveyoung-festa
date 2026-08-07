package com.oliveyoung.festa.order;

import com.oliveyoung.festa.auth.AuthenticatedUser;
import com.oliveyoung.festa.auth.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "spring.data.redis.host=localhost",
        "spring.kafka.bootstrap-servers=localhost:9092"
})
class OrderConcurrencyIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    OrderService orderService;

    @Autowired
    JdbcClient jdbcClient;

    private UUID eventId;
    private UUID gradeId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        gradeId = UUID.randomUUID();
        jdbcClient.sql("""
                        INSERT INTO events (id, name, description, sale_starts_at, sale_ends_at, event_starts_at)
                        VALUES (:id, '동시성 테스트 이벤트', '테스트', CURRENT_TIMESTAMP - INTERVAL '1 minute',
                                CURRENT_TIMESTAMP + INTERVAL '1 hour', CURRENT_TIMESTAMP + INTERVAL '2 hours')
                        """).param("id", eventId).update();
        jdbcClient.sql("""
                        INSERT INTO ticket_grades (id, event_id, code, name, price, currency)
                        VALUES (:id, :eventId, 'GENERAL', '일반', 30000, 'KRW')
                        """).param("id", gradeId).param("eventId", eventId).update();
        jdbcClient.sql("INSERT INTO inventories (ticket_grade_id, total, available) VALUES (:id, 2, 2)")
                .param("id", gradeId).update();
    }

    @Test
    void twoTicketsAllowExactlyTwoConcurrentOrdersAndPreserveInventoryInvariant() throws Exception {
        List<AuthenticatedUser> users = createUsers(100);
        CountDownLatch ready = new CountDownLatch(users.size());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(users.size());
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int index = 0; index < users.size(); index++) {
                AuthenticatedUser user = users.get(index);
                String key = "concurrency-" + index;
                results.add(executor.submit(orderAttempt(user, key, ready, start)));
            }
            ready.await();
            start.countDown();

            long successCount = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    successCount++;
                }
            }

            assertThat(successCount).isEqualTo(2);
            InventoryCounts inventory = jdbcClient.sql("""
                            SELECT total, available, held, sold
                            FROM inventories WHERE ticket_grade_id = :gradeId
                            """).param("gradeId", gradeId)
                    .query((rs, rowNum) -> new InventoryCounts(rs.getInt("total"), rs.getInt("available"),
                            rs.getInt("held"), rs.getInt("sold"))).single();
            assertThat(inventory.available() + inventory.held() + inventory.sold()).isEqualTo(inventory.total());
            assertThat(inventory.held()).isEqualTo(2);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void sameIdempotencyKeyReturnsExistingOrderButDifferentRequestIsRejected() {
        AuthenticatedUser user = createUsers(1).getFirst();

        OrderView first = orderService.createOrder(user, eventId, "GENERAL", "same-key");
        OrderView repeated = orderService.createOrder(user, eventId, "GENERAL", "same-key");

        assertThat(repeated.id()).isEqualTo(first.id());
        assertThatThrownBy(() -> orderService.createOrder(user, eventId, "VIP", "same-key"))
                .isInstanceOf(OrderException.class)
                .extracting(exception -> ((OrderException) exception).status())
                .isEqualTo(com.oliveyoung.festa.api.ApiStatus.IDEMPOTENCY_KEY_CONFLICT);
    }

    @Test
    void oneUserCanHoldOnlyOneOrderAndOrderKeepsPriceAndNameSnapshots() {
        AuthenticatedUser user = createUsers(1).getFirst();
        OrderView created = orderService.createOrder(user, eventId, "GENERAL", "first-key");

        jdbcClient.sql("UPDATE ticket_grades SET name = '변경된 일반', price = 99000 WHERE id = :gradeId")
                .param("gradeId", gradeId).update();
        OrderView stored = orderService.getMyOrders(user).getFirst();

        assertThat(stored.id()).isEqualTo(created.id());
        assertThat(stored.gradeName()).isEqualTo("일반");
        assertThat(stored.unitPrice()).isEqualByComparingTo("30000");
        assertThatThrownBy(() -> orderService.createOrder(user, eventId, "GENERAL", "second-key"))
                .isInstanceOf(OrderException.class)
                .extracting(exception -> ((OrderException) exception).status())
                .isEqualTo(com.oliveyoung.festa.api.ApiStatus.PURCHASE_LIMIT_EXCEEDED);
    }

    private Callable<Boolean> orderAttempt(AuthenticatedUser user, String key, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            try {
                orderService.createOrder(user, eventId, "GENERAL", key);
                return true;
            } catch (OrderException exception) {
                return false;
            }
        };
    }

    private List<AuthenticatedUser> createUsers(int count) {
        List<AuthenticatedUser> users = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            UUID userId = UUID.randomUUID();
            String email = "concurrency-" + userId + "@festa.local";
            jdbcClient.sql("INSERT INTO users (id, email, display_name, role) VALUES (:id, :email, :name, 'CUSTOMER')")
                    .param("id", userId).param("email", email).param("name", "동시성 사용자 " + index).update();
            users.add(new AuthenticatedUser(userId, email, "동시성 사용자 " + index, UserRole.CUSTOMER));
        }
        return users;
    }

    private record InventoryCounts(int total, int available, int held, int sold) {
    }
}
