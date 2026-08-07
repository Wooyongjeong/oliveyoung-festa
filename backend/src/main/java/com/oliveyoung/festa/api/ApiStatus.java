package com.oliveyoung.festa.api;

import org.springframework.http.HttpStatus;

public enum ApiStatus {
    COMMON_SUCCESS(HttpStatus.OK, "COMMON_001", "요청 처리 성공"),
    AUTH_LOGIN_SUCCESS(HttpStatus.OK, "AUTH_001", "로그인 성공"),
    AUTH_ME_SUCCESS(HttpStatus.OK, "AUTH_002", "사용자 조회 성공"),
    EVENT_LIST_SUCCESS(HttpStatus.OK, "EVENT_001", "이벤트 목록 조회 성공"),
    EVENT_DETAIL_SUCCESS(HttpStatus.OK, "EVENT_002", "이벤트 상세 조회 성공"),
    ORDER_CREATE_SUCCESS(HttpStatus.CREATED, "ORDER_001", "주문 생성 성공"),
    ORDER_LIST_SUCCESS(HttpStatus.OK, "ORDER_002", "내 주문 목록 조회 성공"),
    PAYMENT_PROCESS_SUCCESS(HttpStatus.OK, "PAYMENT_001", "결제 처리 성공"),
    PAYMENT_RECONCILE_SUCCESS(HttpStatus.OK, "PAYMENT_002", "결제 상태 조정 성공"),

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "요청 값을 확인해 주세요."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_401", "로그인이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_403", "요청을 수행할 권한이 없습니다."),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT_404", "이벤트를 찾을 수 없습니다."),
    ORDER_NOT_AVAILABLE(HttpStatus.CONFLICT, "ORDER_409", "현재 주문할 수 없습니다."),
    INVENTORY_SOLD_OUT(HttpStatus.CONFLICT, "INVENTORY_409", "선택한 티켓이 매진되었습니다."),
    PURCHASE_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "PURCHASE_409", "이벤트당 하나의 주문만 만들 수 있습니다."),
    IDEMPOTENCY_KEY_CONFLICT(HttpStatus.CONFLICT, "IDEMPOTENCY_409", "같은 멱등성 키에 다른 요청을 사용할 수 없습니다."),
    PAYMENT_NOT_AVAILABLE(HttpStatus.CONFLICT, "PAYMENT_409", "현재 결제를 시작할 수 없습니다."),
    PAYMENT_NOT_RECONCILABLE(HttpStatus.CONFLICT, "PAYMENT_410", "조정할 결제가 없습니다."),
    PAYMENT_RETRY_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "PAYMENT_429", "2초 후 다시 시도해 주세요."),
    PAYMENT_RETRY_EXHAUSTED(HttpStatus.CONFLICT, "PAYMENT_411", "결제 재시도 횟수를 모두 사용했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "요청을 처리하지 못했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ApiStatus(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
