package unischedule.events.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

// 반복 일정 중 단건 삭제 요청 Dto
public record ExpandedRecurringEventDeleteRequestDto(
        @NotNull(message = "삭제하려는 일정의 시작 시간은 필수입니다.")
        @JsonProperty("original_start_time")
        LocalDateTime originalStartTime
) {
}
