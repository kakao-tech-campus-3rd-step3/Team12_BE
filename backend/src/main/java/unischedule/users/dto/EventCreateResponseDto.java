package unischedule.users.dto;

import java.time.LocalDateTime;

public record EventCreateResponseDto(Long eventId, Long userId, String title,
                                     String description, LocalDateTime startTime,
                                     LocalDateTime endTime, boolean isPrivate, String teamId) {
    
}
