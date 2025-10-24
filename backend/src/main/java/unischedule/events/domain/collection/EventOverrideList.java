package unischedule.events.domain.collection;

import unischedule.events.domain.EventOverride;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventOverrideList {
    private final List<EventOverride> eventOverrideList;
    private final Map<LocalDateTime, EventOverride> overrideMap;

    public EventOverrideList(List<EventOverride> overrideList) {
        this.eventOverrideList = new ArrayList<>(overrideList);

        this.overrideMap = this.eventOverrideList.stream()
                .collect(Collectors.toUnmodifiableMap(EventOverride::getOriginalEventTime, ex -> ex));
    }

    public Map<LocalDateTime, EventOverride> getOverrideMap() {
        return overrideMap;
    }

    public boolean isEmpty() {
        return this.eventOverrideList.isEmpty();
    }
}
