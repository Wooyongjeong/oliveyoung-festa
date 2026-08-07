package com.oliveyoung.festa.order;

import com.oliveyoung.festa.api.ApiResponse;
import com.oliveyoung.festa.api.ApiStatus;
import com.oliveyoung.festa.auth.AuthenticatedUser;
import com.oliveyoung.festa.auth.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderView>> create(@LoginUser AuthenticatedUser user,
                                                          @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
                                                          @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.of(ApiStatus.ORDER_CREATE_SUCCESS,
                orderService.createOrder(user, request.eventId(), request.ticketGradeCode(), idempotencyKey));
    }

    @GetMapping("/me/orders")
    public ResponseEntity<ApiResponse<List<OrderView>>> myOrders(@LoginUser AuthenticatedUser user) {
        return ApiResponse.of(ApiStatus.ORDER_LIST_SUCCESS, orderService.getMyOrders(user));
    }

    public record CreateOrderRequest(@NotNull UUID eventId, @NotBlank String ticketGradeCode) {
    }
}
