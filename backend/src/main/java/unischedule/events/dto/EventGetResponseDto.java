package unischedule.events.dto;

import java.time.LocalDateTime;
import unischedule.events.entity.Event;

public record EventGetResponseDto(Long eventId, String title, String description, LocalDateTime startTime,
                                  LocalDateTime endTime, Boolean isPrivate, Long ownerId, Long teamId) {
    public EventGetResponseDto {}
    
    public EventGetResponseDto(Event event) {
        this(event.getId(), event.getTitle(), event.getContent(), event.getStartAt(), event.getEndAt(), event.getIsPrivate(),
            event.getCreatorId(), null);
    }
}
