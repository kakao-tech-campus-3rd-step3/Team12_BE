package unischedule.events.dto;

import java.time.LocalDateTime;
import unischedule.events.entity.Event;

public record EventGetResponseDto(Long eventId, String title, String description, LocalDateTime startTime,
                                  LocalDateTime endTime, Boolean isPrivate) {
    public EventGetResponseDto {}
    
    public EventGetResponseDto(Event event) {
        this(event.getEventId(), event.getTitle(), event.getContent(), event.getStartAt(), event.getEndAt(), event.getIsPrivate());
    }
}
