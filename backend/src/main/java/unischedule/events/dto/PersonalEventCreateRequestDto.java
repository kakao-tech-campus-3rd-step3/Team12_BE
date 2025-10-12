package unischedule.events.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PersonalEventCreateRequestDto(
        @NotBlank
        String title,
        String description,
        @JsonProperty("start_time")
        @NotNull
        LocalDateTime startTime,
        @JsonProperty("end_time")
        @NotNull
        LocalDateTime endTime,
        @JsonProperty("is_private")
        @NotNull
        Boolean isPrivate
) {
    
}
