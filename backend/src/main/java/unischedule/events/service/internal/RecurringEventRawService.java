package unischedule.events.service.internal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventException;
import unischedule.events.repository.EventExceptionRepository;
import unischedule.events.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecurringEventRawService {
    private EventExceptionRepository eventExceptionRepository;
    private EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<Event> findRecurringSchedule(List<Long> calendarIds, LocalDateTime endTime) {
        return eventRepository.findRecurringEventsInPeriod(calendarIds, endTime);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<EventException>> getEventExceptionMap(List<Event> recurringEvents, LocalDateTime startAt, LocalDateTime endAt) {
        return eventExceptionRepository
                .findEventExceptionsForEvents(recurringEvents, startAt, endAt)
                .stream()
                .collect(Collectors.groupingBy(ex -> ex.getOriginalEvent().getEventId()));
    }

    @Transactional
    public void deleteRecurringEvent(Event event) {
        eventExceptionRepository.deleteAllByOriginalEvent(event);
        eventRepository.delete(event);
    }
}
