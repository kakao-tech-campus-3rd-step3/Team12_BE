package unischedule.events.domain.collection;

import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;
import unischedule.events.dto.EventServiceDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExpandedRecurringEvents {
    private final List<Event> expandedEvents;
    private final Event originalEvent;

    private ExpandedRecurringEvents(List<Event> expandedEvents, Event originalEvent) {
        this.expandedEvents = expandedEvents;
        this.originalEvent = originalEvent;
    }

    public static ExpandedRecurringEvents of(List<Event> expandedEvents, Event originalEvent) {
        if (originalEvent == null || originalEvent.getRecurrenceRule() == null) {
            throw new IllegalArgumentException("원본 이벤트는 반복 규칙을 가지는 유효한 객체여야 합니다.");
        }

        List<Event> events = (expandedEvents == null)
                ? Collections.emptyList()
                : List.copyOf(expandedEvents);

        return new ExpandedRecurringEvents(events, originalEvent);
    }

    public List<Event> applyOverrides(EventOverrideSeries overrideList) {
        if (overrideList == null || overrideList.isEmpty()) {
            return this.expandedEvents;
        }

        Map<LocalDateTime, EventOverride> overrideMap = overrideList.getOverrideMap();

        List<Event> finalEventList = new ArrayList<>();

        for (Event event : expandedEvents) {
            applyOverrideToEvent(event, overrideMap)
                    .ifPresent(finalEventList::add);
        }
        return List.copyOf(finalEventList);
    }

    private Optional<Event> applyOverrideToEvent(Event event, Map<LocalDateTime, EventOverride> overrideMap) {
        EventOverride eventOverride = overrideMap.get(event.getStartAt());

        if (eventOverride == null) {
            return Optional.of(event);
        }

        if (eventOverride.isDeleteOverride()) {
            return Optional.empty();
        }

        return Optional.of(eventOverride.toEvent());
    }

    public List<EventServiceDto> applyOverridesToDtos(EventOverrideSeries overrideList) {
        List<Event> finalEvents = applyOverrides(overrideList);
        return finalEvents.stream()
                .map(event -> EventServiceDto.fromRecurringEvent(event, true, this.originalEvent))
                .toList();
    }

    public boolean isEmpty() {
        return this.expandedEvents.isEmpty();
    }
}
