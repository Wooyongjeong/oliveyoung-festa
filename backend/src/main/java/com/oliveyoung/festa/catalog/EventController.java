package com.oliveyoung.festa.catalog;

import com.oliveyoung.festa.auth.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepository;
    private final CurrentUser currentUser;

    public EventController(EventRepository eventRepository, CurrentUser currentUser) {
        this.eventRepository = eventRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<EventSummary> list() {
        currentUser.requireAuthenticated();
        return eventRepository.findAll();
    }

    @GetMapping("/{eventId}")
    public EventDetail detail(@PathVariable UUID eventId) {
        currentUser.requireAuthenticated();
        return eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
    }
}
