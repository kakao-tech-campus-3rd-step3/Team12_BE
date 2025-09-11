package unischedule.events.dto;

import java.time.LocalDateTime;

public record EventModifyRequestDto(String title, String description, LocalDateTime startTime,
                                    LocalDateTime endTime, boolean isPrivate) {
    
}
