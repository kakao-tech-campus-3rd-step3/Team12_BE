package unischedule.events.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import unischedule.events.validation.ValidEventTime;

import java.time.LocalDateTime;

@ValidEventTime
public record EventModifyRequestDto(
        @JsonProperty("event_id")
        @NotNull
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
        public static EventUpdateDto toDto(EventModifyRequestDto requestDto) {
                return new EventUpdateDto(
                        requestDto.title,
                        requestDto.description,
                        requestDto.startTime,
                        requestDto.endTime,
                        requestDto.isPrivate
                );
        }
    
}
