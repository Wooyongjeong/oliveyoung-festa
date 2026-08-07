package com.oliveyoung.festa.catalog;

import java.util.UUID;

public class EventNotFoundException extends RuntimeException {

    private final UUID eventId;

    public EventNotFoundException(UUID eventId) {
        super("이벤트를 찾을 수 없습니다.");
        this.eventId = eventId;
    }

    public UUID eventId() {
        return eventId;
    }
}
