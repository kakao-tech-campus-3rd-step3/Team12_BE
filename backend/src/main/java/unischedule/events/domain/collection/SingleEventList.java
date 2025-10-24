package unischedule.events.domain.collection;

import unischedule.events.domain.Event;
import unischedule.events.dto.EventServiceDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SingleEventList {

    private final List<Event> eventList;
    private final Boolean isRecurring = false;

    public SingleEventList(List<Event> eventList) {
        for (Event event : eventList) {
            if (event.getRecurrenceRule() != null) {
                throw new IllegalArgumentException("반복 일정을 단일 일정으로 관리할 수 없습니다.");
            }
        }

        this.eventList = new ArrayList<>(eventList);
    }

    public boolean hasOverlap(LocalDateTime startAt, LocalDateTime endAt) {
        return eventList.stream().anyMatch(event ->
                event.getEndAt().isAfter(startAt) && event.getStartAt().isBefore(endAt)
        );
    }

    public List<EventServiceDto> toServiceDtos() {
        return eventList.stream()
                .map(event -> EventServiceDto.fromSingleEvent(event, false))
                .toList();
    }

    public boolean isEmpty() {
        return this.eventList.isEmpty();
    }

    public List<Event> getEvents() {
        return eventList;
    }
}
