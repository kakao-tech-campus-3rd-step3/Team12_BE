package unischedule.events.dto;

import java.time.LocalDateTime;

public record EventOverrideUpdateDto(
        String title,
        String content,
        LocalDateTime startTime,
        LocalDateTime endTime
) {

}
