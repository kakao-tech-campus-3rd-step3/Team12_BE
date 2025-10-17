package unischedule.events.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RecurringEventCreateRequestDto(
        @NotBlank(message = "제목은 필수입니다.")
        String title,
        String description,
        @JsonProperty("first_start_time")
        // 첫 시작 시간(반복 기준점)
        @NotNull(message = "시작 시간은 필수입니다.")
        LocalDateTime firstStartTime,
        @JsonProperty("first_end_time")
        @NotNull(message = "종료 시간은 필수입니다.")
        LocalDateTime firstEndTime,
        @JsonProperty("is_private")
        @NotNull(message = "공개 여부는 필수입니다.")
        Boolean isPrivate,
        @NotBlank(message = "반복 규칙(rrule)은 필수입니다.")
        String rrule
) {
}
