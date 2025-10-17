package unischedule.events.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import unischedule.events.domain.Event;

import java.time.LocalDateTime;

public record EventGetResponseDto(
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
    public static EventGetResponseDto from(Event event) {
        return new EventGetResponseDto(
                event.getEventId(),
                event.getTitle(),
                event.getContent(),
                event.getStartAt(),
                event.getEndAt(),
                event.getIsPrivate()
        );
    }
}
