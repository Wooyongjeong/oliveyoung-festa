package com.oliveyoung.festa.api;

import org.springframework.http.HttpStatus;

public enum ApiStatus {
    COMMON_SUCCESS(HttpStatus.OK, "COMMON_001", "요청 처리 성공"),
    AUTH_LOGIN_SUCCESS(HttpStatus.OK, "AUTH_001", "로그인 성공"),
    AUTH_ME_SUCCESS(HttpStatus.OK, "AUTH_002", "사용자 조회 성공"),
    EVENT_LIST_SUCCESS(HttpStatus.OK, "EVENT_001", "이벤트 목록 조회 성공"),
    EVENT_DETAIL_SUCCESS(HttpStatus.OK, "EVENT_002", "이벤트 상세 조회 성공"),

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "요청 값을 확인해 주세요."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_401", "로그인이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_403", "요청을 수행할 권한이 없습니다."),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT_404", "이벤트를 찾을 수 없습니다."),
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
