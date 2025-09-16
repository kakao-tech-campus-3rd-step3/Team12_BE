package unischedule.events.dto;

import java.time.LocalDateTime;
import unischedule.events.entity.Event;

public record EventCreateResponseDto(Long eventId, String title,
                                     String description, LocalDateTime startTime,
                                     LocalDateTime endTime, boolean isPrivate, String teamId) {
    
    public EventCreateResponseDto {}
    
    public EventCreateResponseDto(Event event) {
        this(event.getEventId(), event.getTitle(), event.getContent(), event.getStartAt(), event.getEndAt(),
            event.getIsPrivate(), null);
    }
    
}
