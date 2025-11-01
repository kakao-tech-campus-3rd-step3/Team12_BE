package unischedule.events.service.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;
import unischedule.events.domain.collection.EventOverrideSeries;
import unischedule.events.domain.collection.ExpandedRecurringEvents;
import unischedule.events.domain.collection.RecurringEventSeries;
import unischedule.events.dto.EventServiceDto;
import unischedule.events.service.internal.RecurringEventRawService;
import unischedule.events.util.RRuleParser;
import unischedule.member.domain.Member;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecurringEventService {
    private final RecurringEventRawService recurringEventRawService;
    private final RRuleParser rruleParser;

    @Transactional(readOnly = true)
    public List<EventServiceDto> expandRecurringEvents(List<Long> calendarIds, LocalDateTime startAt, LocalDateTime endAt) {
        RecurringEventSeries recurringEvents = recurringEventRawService.findRecurringSchedule(calendarIds, endAt);

        return getEventServiceDtos(startAt, endAt, recurringEvents);
    }

    @Transactional(readOnly = true)
    public List<EventServiceDto> expandRecurringEventsForMember(Member member, List<Long> calendarIds, LocalDateTime startAt, LocalDateTime endAt) {
        RecurringEventSeries recurringEvents = recurringEventRawService.findRecurringScheduleForMember(member, calendarIds, endAt);

        return getEventServiceDtos(startAt, endAt, recurringEvents);
    }

    private List<EventServiceDto> getEventServiceDtos(LocalDateTime startAt, LocalDateTime endAt, RecurringEventSeries recurringEvents) {
        if (recurringEvents.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventServiceDto> finalExpandedEventList = new ArrayList<>();

        Map<Long, List<EventOverride>> exceptionsMap = recurringEventRawService.getEventOverrideMap(recurringEvents.getEvents(), startAt, endAt);

        for (Event recurEvent : recurringEvents.getEvents()) {
            List<Event> expandedEvent = expandRecurringEvent(recurEvent, startAt, endAt);
            ExpandedRecurringEvents expandedRecurringEvents = ExpandedRecurringEvents.of(expandedEvent, recurEvent);
            EventOverrideSeries overrideSeries = new EventOverrideSeries(exceptionsMap.getOrDefault(recurEvent.getEventId(), List.of()));

            finalExpandedEventList.addAll(expandedRecurringEvents.applyOverridesToDtos(overrideSeries));
        }

        return finalExpandedEventList;
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
                        .build())
                .toList();
    }
}
