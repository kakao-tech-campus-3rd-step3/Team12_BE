package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.dto.EventServiceDto;
import unischedule.events.service.internal.EventRawService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventQueryService {
    private final EventRawService eventRawService;
    private final RecurringEventService recurringEventService;

    @Transactional(readOnly = true)
    public List<EventServiceDto> getEvents(List<Long> calendarIds, LocalDateTime startAt, LocalDateTime endAt) {
        List<EventServiceDto> eventList = new ArrayList<>();
        eventList.addAll(eventRawService.findSingleSchedule(calendarIds, startAt, endAt));
        eventList.addAll(recurringEventService.expandRecurringEvents(calendarIds, startAt, endAt));

        return eventList;
    }
}
