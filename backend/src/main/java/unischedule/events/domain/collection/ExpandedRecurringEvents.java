package unischedule.events.domain.collection;

import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;
import unischedule.events.dto.EventServiceDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExpandedRecurringEvents {
    private final List<Event> expandedEvents;
    private final Event originalEvent;

    public ExpandedRecurringEvents(List<Event> expandedEvents, Event originalEvent) {
        if (originalEvent == null || originalEvent.getRecurrenceRule() == null) {
            throw new IllegalArgumentException("원본 이벤트는 반복 규칙을 가지는 유효한 객체여야 합니다.");
        }
        this.originalEvent = originalEvent;

        if (expandedEvents == null) {
            this.expandedEvents = Collections.emptyList();
        }
        else {
            this.expandedEvents = List.copyOf(expandedEvents);
        }
    }

    public List<Event> applyOverrides(EventOverrideList overrideList) {
        if (overrideList == null || overrideList.isEmpty()) {
            return this.expandedEvents;
        }

        Map<LocalDateTime, EventOverride> overrideMap = overrideList.getOverrideMap();

        List<Event> finalEventList = new ArrayList<>();

        for (Event event : expandedEvents) {
            if (overrideMap.containsKey(event.getStartAt())) {
                EventOverride eventOverride = overrideMap.get(event.getStartAt());

                if (eventOverride.isDeleteOverride()) {
                    continue;
                }

                finalEventList.add(eventOverride.toEvent());
            }
            else {
                finalEventList.add(event);
            }
        }
        return List.copyOf(finalEventList);
    }

    public List<EventServiceDto> applyOverridesToDtos(EventOverrideList overrideList) {
        List<Event> finalEvents = applyOverrides(overrideList);
        return finalEvents.stream()
                .map(event -> EventServiceDto.fromRecurringEvent(event, true, this.originalEvent))
                .toList();
    }

    public boolean isEmpty() {
        return this.expandedEvents.isEmpty();
    }
}
