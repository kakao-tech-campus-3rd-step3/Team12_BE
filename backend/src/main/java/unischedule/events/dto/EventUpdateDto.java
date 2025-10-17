package unischedule.events.dto;

import java.time.LocalDateTime;

public record EventUpdateDto(
        String title,
        String content,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean isPrivate
) {
}
