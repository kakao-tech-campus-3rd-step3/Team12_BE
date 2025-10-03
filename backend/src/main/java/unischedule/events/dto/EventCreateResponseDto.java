package unischedule.events.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import unischedule.events.domain.Event;

public record EventCreateResponseDto(
        @JsonProperty("event_id")
        Long eventId,
        String title,
        String description,
        @JsonProperty("start_time")
        LocalDateTime startTime,
        @JsonProperty("end_time")
        LocalDateTime endTime,
        @JsonProperty("is_private")
        Boolean isPrivate
) {
    public static EventCreateResponseDto from(Event event) {
        return new EventCreateResponseDto(
                event.getEventId(),
                event.getTitle(),
                event.getContent(),
                event.getStartAt(),
                event.getEndAt(),
                event.getIsPrivate()
        );
    }
    
}
