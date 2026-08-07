package com.oliveyoung.festa.payment;

import com.oliveyoung.festa.api.ApiResponse;
import com.oliveyoung.festa.api.ApiStatus;
import com.oliveyoung.festa.auth.AuthenticatedUser;
import com.oliveyoung.festa.auth.LoginUser;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders/{orderId}/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentView>> pay(@LoginUser AuthenticatedUser user,
            @PathVariable UUID orderId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = "X-Mock-Scenario", required = false) String scenario) {
        return ApiResponse.of(ApiStatus.PAYMENT_PROCESS_SUCCESS, service.pay(user, orderId, idempotencyKey, scenario));
    }

    @PostMapping("/reconcile")
    public ResponseEntity<ApiResponse<PaymentView>> reconcile(@LoginUser AuthenticatedUser user,
            @PathVariable UUID orderId) {
        return ApiResponse.of(ApiStatus.PAYMENT_RECONCILE_SUCCESS, service.reconcile(user, orderId));
    }
}
