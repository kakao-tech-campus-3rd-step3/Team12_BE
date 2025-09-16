package unischedule.events.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record EventCreateRequestDto(
    @NotBlank String title, @NotBlank String description, @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime, boolean isPrivate) {
    
}
