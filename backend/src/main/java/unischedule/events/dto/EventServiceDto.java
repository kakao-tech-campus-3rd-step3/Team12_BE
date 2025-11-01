package unischedule.events.dto;

import unischedule.events.domain.Event;

import java.time.LocalDateTime;

public record EventServiceDto(
        Long eventId,
        String title,
        String content,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Boolean fromRecurring
) {
    public static EventServiceDto fromSingleEvent(Event event, Boolean fromRecurring) {
        return new EventServiceDto(
                event.getEventId(),
                event.getTitle(),
                event.getContent(),
                event.getStartAt(),
                event.getEndAt(),
                fromRecurring
        );
    }

    public static EventServiceDto fromRecurringEvent(Event event, Boolean fromRecurring, Event originalEvent) {
        return new EventServiceDto(
                originalEvent.getEventId(),
                event.getTitle(),
                event.getContent(),
                event.getStartAt(),
                event.getEndAt(),
                fromRecurring
        );
    }
}
