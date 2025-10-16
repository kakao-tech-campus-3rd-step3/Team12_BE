package unischedule.events.dto;

import unischedule.events.domain.Event;
import unischedule.events.domain.EventState;

import java.time.LocalDateTime;

public record EventServiceDto(
        Long eventId,
        String title,
        String content,
        LocalDateTime startAt,
        LocalDateTime endAt,
        EventState state,
        Boolean isPrivate,
        Boolean fromRecurring
) {
    public static EventServiceDto from(Event event, Boolean fromRecurring) {
        return new EventServiceDto(
                event.getEventId(),
                event.getTitle(),
                event.getContent(),
                event.getStartAt(),
                event.getEndAt(),
                event.getState(),
                event.getIsPrivate(),
                fromRecurring
        );
    }
}
