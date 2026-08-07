package com.oliveyoung.festa.mockpg;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:mockpg;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class MockPaymentControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcClient jdbc;

    @BeforeEach void clean() {
        jdbc.sql("DELETE FROM mock_refunds").update();
        jdbc.sql("DELETE FROM mock_payments").update();
    }

    @Test
    void approvalIsPersistentAndIdempotent() throws Exception {
        String body = "{\"attemptKey\":\"attempt-1\",\"amount\":30000,\"currency\":\"KRW\"}";
        mockMvc.perform(post("/mock-api/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(get("/mock-api/payments/attempt-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(30000));
        mockMvc.perform(post("/mock-api/payments").header("X-Mock-Scenario", "DECLINE")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void deterministicDeclineIsSupported() throws Exception {
        mockMvc.perform(post("/mock-api/payments").header("X-Mock-Scenario", "DECLINE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attemptKey\":\"attempt-2\",\"amount\":30000,\"currency\":\"KRW\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DECLINED"));
    }
}
