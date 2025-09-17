package unischedule.events.dto;

import java.time.LocalDateTime;

public record EventModifyRequestDto(
        Long eventId,
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean isPrivate
) {
    
}
