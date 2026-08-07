package com.oliveyoung.festa.order;

import com.oliveyoung.festa.api.ApiStatus;
import com.oliveyoung.festa.auth.AuthenticatedUser;
import com.oliveyoung.festa.auth.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderView createOrder(AuthenticatedUser user, UUID eventId, String ticketGradeCode, String idempotencyKey) {
        requireCustomer(user);
        String requestHash = requestHash(eventId, ticketGradeCode);
        var existing = orderRepository.findByIdempotencyKey(user.id(), idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().requestHash().equals(requestHash)) {
                throw new OrderException(ApiStatus.IDEMPOTENCY_KEY_CONFLICT);
            }
            return orderRepository.findOrder(existing.get().id()).orElseThrow();
        }

        var grade = orderRepository.findGradeForSale(eventId, ticketGradeCode)
                .orElseThrow(() -> new OrderException(ApiStatus.ORDER_NOT_AVAILABLE));
        UUID orderId = UUID.randomUUID();
        orderRepository.insertOrder(orderId, user.id(), grade, idempotencyKey, requestHash);
        orderRepository.ensurePurchaseRight(user.id(), eventId);
        if (!orderRepository.claimPurchaseRight(user.id(), eventId, orderId)) {
            throw new OrderException(ApiStatus.PURCHASE_LIMIT_EXCEEDED);
        }
        if (!orderRepository.holdInventory(grade.ticketGradeId())) {
            throw new OrderException(ApiStatus.INVENTORY_SOLD_OUT);
        }
        orderRepository.insertReservation(UUID.randomUUID(), orderId);
        orderRepository.insertHistory(UUID.randomUUID(), orderId);
        return orderRepository.findOrder(orderId).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<OrderView> getMyOrders(AuthenticatedUser user) {
        requireCustomer(user);
        return orderRepository.findOrdersByUser(user.id());
    }

    private void requireCustomer(AuthenticatedUser user) {
        if (user.role() != UserRole.CUSTOMER) {
            throw new OrderException(ApiStatus.PURCHASE_LIMIT_EXCEEDED);
        }
    }

    private String requestHash(UUID eventId, String ticketGradeCode) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((eventId + ":" + ticketGradeCode).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
