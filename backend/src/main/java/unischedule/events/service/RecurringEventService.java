package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import net.fortuna.ical4j.model.Recur;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;
import unischedule.events.dto.EventServiceDto;
import unischedule.events.service.internal.RecurringEventRawService;
import unischedule.events.util.RRuleParser;
import unischedule.events.util.ZonedDateTimeUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecurringEventService {
    private final RecurringEventRawService recurringEventRawService;
    private final RRuleParser rruleParser;
    private final ZonedDateTimeUtil zonedDateTimeUtil;

    private final Boolean fromRecurring = true;

    @Transactional(readOnly = true)
    public List<EventServiceDto> expandRecurringEvents(List<Long> calendarIds, LocalDateTime startAt, LocalDateTime endAt) {
        List<Event> recurringEvents = recurringEventRawService.findRecurringSchedule(calendarIds, endAt);

        List<EventServiceDto> expandedEventList = new ArrayList<>();

        Map<Long, List<EventOverride>> exceptionsMap = recurringEventRawService.getEventOverrideMap(recurringEvents, startAt, endAt);

        for (Event recurEvent : recurringEvents) {
            List<Event> expandedEvent = expandRecurringEvent(recurEvent, startAt, endAt);
            List<EventOverride> overrides = exceptionsMap.getOrDefault(recurEvent.getEventId(), List.of());

            applyEventOverrides(expandedEvent, overrides)
                    .stream()
                    .map(event -> EventServiceDto.fromRecurringEvent(event, fromRecurring, recurEvent))
                    .forEach(expandedEventList::add);
        }

        return expandedEventList;
    }

    private List<Event> expandRecurringEvent(Event recEvent, LocalDateTime startAt, LocalDateTime endAt) {
        Recur<ZonedDateTime> recur = rruleParser.getRecur(recEvent.getRecurrenceRule().getRruleString());

        ZonedDateTime seed = zonedDateTimeUtil.localDateTimeToZdt(recEvent.getStartAt());
        ZonedDateTime startZdt = zonedDateTimeUtil.localDateTimeToZdt(startAt);
        ZonedDateTime endZdt = zonedDateTimeUtil.localDateTimeToZdt(endAt);

        Duration duration = Duration.between(seed.toLocalDateTime(), recEvent.getEndAt());

        List<ZonedDateTime> dates = recur.getDates(startZdt, endZdt);

        return dates.stream()
                .filter(eventStart -> !eventStart.isBefore(startZdt) && eventStart.isBefore(endZdt))
                .map(eventStart -> Event.builder()
                        .title(recEvent.getTitle())
                        .content(recEvent.getContent())
                        .startAt(eventStart.toLocalDateTime())
                        .endAt(eventStart.toLocalDateTime().plus(duration))
                        .isPrivate(recEvent.getIsPrivate())
                        .state(recEvent.getState())
                        .build())
                .toList();
    }

    private List<Event> applyEventOverrides(List<Event> expandedEvents, List<EventOverride> exceptions) {
        if (exceptions.isEmpty()) {
            return expandedEvents;
        }

        Map<LocalDateTime, EventOverride> exceptionMap = exceptions.stream()
                .collect(Collectors.toMap(EventOverride::getOriginalEventTime, ex -> ex));

        List<Event> finalEvents = new ArrayList<>();

        for (Event event : expandedEvents) {
            if (exceptionMap.containsKey(event.getStartAt())) {
                EventOverride exception = exceptionMap.get(event.getStartAt());

                if (isDeletionException(exception)) {
                    continue;
                }

                finalEvents.add(applyOverride(exception));
            }
            else {
                finalEvents.add(event);
            }
        }
        return finalEvents;
    }

    private boolean isDeletionException(EventOverride eventOverride) {
        return eventOverride.getTitle() == null;
    }

    private Event applyOverride(EventOverride eventOverride) {
        return Event.builder()
                .title(eventOverride.getTitle())
                .content(eventOverride.getContent())
                .startAt(eventOverride.getStartAt())
                .endAt(eventOverride.getEndAt())
                .isPrivate(eventOverride.getIsPrivate())
                .build();
    }
}
