package unischedule.events.domain.collection;

import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventOverrideList {
    private List<EventOverride> eventOverrideList;

    public EventOverrideList(List<EventOverride> overrideList) {
        this.eventOverrideList = new ArrayList<>(overrideList);
    }

    public List<Event> applyEventOverrides(List<Event> expandedEventList) {
        if (eventOverrideList.isEmpty()) {
            return expandedEventList;
        }

        Map<LocalDateTime, EventOverride> overrideMap = eventOverrideList.stream()
                .collect(Collectors.toMap(EventOverride::getOriginalEventTime, ex -> ex));

        List<Event> finalEventList = new ArrayList<>();
        for (Event event : expandedEventList) {
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

        return finalEventList;
    }
}
