package unischedule.events.dto;

import java.time.LocalDateTime;

public record EventCreateDto(
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean isPrivate
) {
}
