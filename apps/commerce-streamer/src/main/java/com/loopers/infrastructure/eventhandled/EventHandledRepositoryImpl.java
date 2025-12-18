package com.loopers.infrastructure.eventhandled;

import com.loopers.domain.eventhandled.EventHandled;
import com.loopers.domain.eventhandled.EventHandledRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class EventHandledRepositoryImpl implements EventHandledRepository {

    private final EventHandledJpaRepository eventHandledJpaRepository;

    @Override
    public void saveEventHandled(EventHandled eventHandled) {
        eventHandledJpaRepository.save(eventHandled);
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return eventHandledJpaRepository.existsByEventId(eventId);
    }
}

