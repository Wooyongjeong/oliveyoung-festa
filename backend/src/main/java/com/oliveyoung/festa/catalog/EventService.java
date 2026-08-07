package com.oliveyoung.festa.catalog;

import com.oliveyoung.festa.auth.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final CurrentUser currentUser;

    public EventService(EventRepository eventRepository, CurrentUser currentUser) {
        this.eventRepository = eventRepository;
        this.currentUser = currentUser;
    }

    public List<EventSummary> getEvents() {
        currentUser.requireAuthenticated();
        return eventRepository.findAll();
    }

    public EventDetail getEvent(UUID eventId) {
        currentUser.requireAuthenticated();
        return eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
    }
}
