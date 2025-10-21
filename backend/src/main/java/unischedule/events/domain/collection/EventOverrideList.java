package unischedule.events.domain.collection;

import unischedule.events.domain.EventOverride;

import java.util.ArrayList;
import java.util.List;

public class EventOverrideList {
    private List<EventOverride> eventOverrideList;

    public EventOverrideList(List<EventOverride> overrideList) {
        this.eventOverrideList = new ArrayList<>(overrideList);
    }

}
