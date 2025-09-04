package unischedule.users.dto;

import java.time.LocalDateTime;

public record EventGetResponseDto(Long eventId, String title, String description, LocalDateTime startTime,
                                  LocalDateTime endTime, Boolean isPrivate, Long ownerId, Long teamId) {
    
}
