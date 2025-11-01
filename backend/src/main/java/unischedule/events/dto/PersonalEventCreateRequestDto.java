package unischedule.events.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PersonalEventCreateRequestDto(
        @NotBlank(message = "제목은 필수입니다.")
        String title,
        String description,
        @JsonProperty("start_time")
        @NotNull(message = "시작 시간은 필수입니다.")
        LocalDateTime startTime,
        @JsonProperty("end_time")
        @NotNull(message = "종료 시간은 필수입니다.")
        LocalDateTime endTime
) {
        public EventCreateDto toDto() {
                return new EventCreateDto(
                        this.title,
                        this.description,
                        this.startTime,
                        this.endTime
                );
        }
    
}
