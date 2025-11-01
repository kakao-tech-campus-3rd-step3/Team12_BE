package unischedule.events.domain.collection;

import lombok.Getter;
import unischedule.events.domain.EventOverride;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class EventOverrideSeries {
    private final List<EventOverride> eventOverrideList;
    private final Map<LocalDateTime, EventOverride> overrideMap;

    public EventOverrideSeries(List<EventOverride> overrideList) {
        this.eventOverrideList = new ArrayList<>(overrideList);

        this.overrideMap = this.eventOverrideList.stream()
                .collect(Collectors.toUnmodifiableMap(EventOverride::getOriginalEventTime, ex -> ex));
    }

    public boolean isEmpty() {
        return this.eventOverrideList.isEmpty();
    }
}
