package unischedule.events.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import unischedule.events.entity.Event;

public record EventCreateResponseDto(
        Long eventId,
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean isPrivate
) {
    public static EventCreateResponseDto from(Event event) {
        return new EventCreateResponseDto(
                event.getEventId(),
                event.getTitle(),
                event.getContent(),
                event.getStartAt(),
                event.getEndAt(),
                event.getIsPrivate()
        );
    }
    
}
