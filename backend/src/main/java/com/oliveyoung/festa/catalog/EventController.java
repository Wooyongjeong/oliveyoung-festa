package com.oliveyoung.festa.catalog;

import com.oliveyoung.festa.api.ApiResponse;
import com.oliveyoung.festa.api.ApiStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventSummary>>> list() {
        return ApiResponse.of(ApiStatus.EVENT_LIST_SUCCESS, eventService.getEvents());
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventDetail>> detail(@PathVariable UUID eventId) {
        return ApiResponse.of(ApiStatus.EVENT_DETAIL_SUCCESS, eventService.getEvent(eventId));
    }
}
