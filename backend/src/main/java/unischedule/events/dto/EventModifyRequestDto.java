package unischedule.events.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import unischedule.events.validation.ValidEventTime;

import java.time.LocalDateTime;
import java.util.List;

@ValidEventTime
public record EventModifyRequestDto(
        String title,
        String description,
        @JsonProperty("start_time")
        LocalDateTime startTime,
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @JsonProperty("event_participants")
        List<Long> eventParticipants
) {
        public EventUpdateDto toDto() {
                return new EventUpdateDto(
                        this.title,
                        this.description,
                        this.startTime,
                        this.endTime
                );
        }
    
}
