package unischedule.events.dto;

import java.time.LocalDateTime;

public record EventExceptionDto(
        LocalDateTime originalStartTime,
        String title,
        String content,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean isPrivate
) {
}
