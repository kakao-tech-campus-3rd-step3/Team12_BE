package unischedule.events.domain.collection;

import unischedule.events.domain.Event;

import java.util.ArrayList;
import java.util.List;

public class RecurringEventList {
    private final List<Event> eventList;
    public RecurringEventList(List<Event> eventList) {
        for (Event event : eventList) {
            if (event.getRecurrenceRule() == null) {
                throw new IllegalArgumentException("단일 일정을 반복 일정으로 관리할 수 없습니다.");
            }
        }

        this.eventList = new ArrayList<>(eventList);
    }

    public boolean isEmpty() {
        return this.eventList.isEmpty();
    }

    public List<Event> getEvents() {
        return this.eventList;
    }
}
