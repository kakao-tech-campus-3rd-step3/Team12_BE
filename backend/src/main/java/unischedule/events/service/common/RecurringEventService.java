package unischedule.events.service.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;
import unischedule.events.domain.collection.EventOverrideList;
import unischedule.events.dto.EventServiceDto;
import unischedule.events.service.internal.RecurringEventRawService;
import unischedule.events.util.RRuleParser;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecurringEventService {
    private final RecurringEventRawService recurringEventRawService;
    private final RRuleParser rruleParser;

    private final static Boolean fromRecurring = true;

    @Transactional(readOnly = true)
    public List<EventServiceDto> expandRecurringEvents(List<Long> calendarIds, LocalDateTime startAt, LocalDateTime endAt) {
        List<Event> recurringEvents = recurringEventRawService.findRecurringSchedule(calendarIds, endAt);

        List<EventServiceDto> expandedEventList = new ArrayList<>();

        Map<Long, List<EventOverride>> exceptionsMap = recurringEventRawService.getEventOverrideMap(recurringEvents, startAt, endAt);

        for (Event recurEvent : recurringEvents) {
            List<Event> expandedEvent = expandRecurringEvent(recurEvent, startAt, endAt);
            EventOverrideList overrideList = new EventOverrideList(exceptionsMap.getOrDefault(recurEvent.getEventId(), List.of()));

            overrideList.applyEventOverrides(expandedEvent)
                    .stream()
                    .map(event -> EventServiceDto.fromRecurringEvent(event, fromRecurring, recurEvent))
                    .forEach(expandedEventList::add);
        }

        return expandedEventList;
    }

    private List<Event> expandRecurringEvent(Event recEvent, LocalDateTime startAt, LocalDateTime endAt) {
        List<LocalDateTime> dates = rruleParser.calEventStartTimeListRange(
                recEvent.getRecurrenceRule().getRruleString(),
                recEvent.getStartAt(),
                startAt,
                endAt
        );
        Duration duration = Duration.between(recEvent.getStartAt(), recEvent.getEndAt());

        return dates.stream()
                .map(eventStart -> Event.builder()
                        .title(recEvent.getTitle())
                        .content(recEvent.getContent())
                        .startAt(eventStart)
                        .endAt(eventStart.plus(duration))
                        .isPrivate(recEvent.getIsPrivate())
                        .state(recEvent.getState())
                        .build())
                .toList();
    }
}
