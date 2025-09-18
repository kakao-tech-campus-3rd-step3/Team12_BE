package unischedule.events.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record EventModifyRequestDto(
        @NotNull
        Long eventId,
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean isPrivate
) {
    
}
