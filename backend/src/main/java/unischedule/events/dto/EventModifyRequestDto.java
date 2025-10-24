package unischedule.events.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import unischedule.events.validation.ValidEventTime;

import java.time.LocalDateTime;

@ValidEventTime
public record EventModifyRequestDto(
        String title,
        String description,
        @JsonProperty("start_time")
        LocalDateTime startTime,
        @JsonProperty("end_time")
        LocalDateTime endTime,
        @JsonProperty("is_private")
        Boolean isPrivate
) {
        public EventUpdateDto toDto() {
                return new EventUpdateDto(
                        this.title,
                        this.description,
                        this.startTime,
                        this.endTime,
                        this.isPrivate
                );
        }
    
}
