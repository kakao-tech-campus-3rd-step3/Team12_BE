package unischedule.users.dto;

import java.time.LocalDateTime;

public record EventCreateRequestDto(String title, String description, LocalDateTime startTime,
                                    LocalDateTime endTime, boolean isPrivate) {
    
}
