package unischedule.events.dto;

import java.time.LocalDateTime;
import unischedule.events.entity.Event;

public record EventGetResponseDto(
        Long eventId,
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean isPrivate
) {
    public static EventGetResponseDto from(Event event) {
        return new EventGetResponseDto(
                event.getEventId(),
                event.getTitle(),
                event.getContent(),
                event.getStartAt(),
                event.getEndAt(),
                event.getIsPrivate()
        );
    }
}
