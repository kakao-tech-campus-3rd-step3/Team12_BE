package unischedule.events.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;
import unischedule.events.repository.EventOverrideRepository;
import unischedule.events.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecurringEventRawService {
    private final EventOverrideRepository eventOverrideRepository;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<Event> findRecurringSchedule(List<Long> calendarIds, LocalDateTime endTime) {
        return eventRepository.findRecurringEventsInPeriod(calendarIds, endTime);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<EventOverride>> getEventOverrideMap(List<Event> recurringEvents, LocalDateTime startAt, LocalDateTime endAt) {
        return eventOverrideRepository
                .findEventOverridesForEvents(recurringEvents, startAt, endAt)
                .stream()
                .collect(Collectors.groupingBy(ex -> ex.getOriginalEvent().getEventId()));
    }

    @Transactional
    public void deleteRecurringEvent(Event event) {
        eventOverrideRepository.deleteAllByOriginalEvent(event);
        eventRepository.delete(event);
    }
}
