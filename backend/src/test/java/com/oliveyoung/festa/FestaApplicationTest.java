package com.oliveyoung.festa;

import com.oliveyoung.festa.auth.UserRepository;
import com.oliveyoung.festa.catalog.EventRepository;
import com.oliveyoung.festa.order.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class FestaApplicationTest {

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    EventRepository eventRepository;

    @MockitoBean
    OrderRepository orderRepository;

    @Test
    void contextLoads() {
    }
}
