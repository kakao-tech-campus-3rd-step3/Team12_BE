package unischedule.events.dto;

import java.time.LocalDateTime;

public record EventModifyDto(
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean isPrivate
) {
    public static EventUpdateDto toDto(EventModifyDto modifyDto) {
        return new EventUpdateDto(
                modifyDto.title,
                modifyDto.description,
                modifyDto.startTime,
                modifyDto.endTime,
                modifyDto.isPrivate
        );
    }
}
