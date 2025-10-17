package unischedule.events.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import unischedule.events.validation.ValidEventTime;

import java.time.LocalDateTime;

// 반복 일정 중 단건 수정 요청 Dto
@ValidEventTime
public record RecurringInstanceModifyRequestDto(
        @NotNull(message = "수정하려는 일정의 시작 시간은 필수입니다.")
        @JsonProperty("original_start_time")
        LocalDateTime originalStartTime,
        String title,
        String content,
        @JsonProperty("start_time")
        LocalDateTime startTime,
        @JsonProperty("end_time")
        LocalDateTime endTime,
        @JsonProperty("is_private")
        Boolean isPrivate
) {
    public EventOverrideDto toEventOverrideDto() {
        return new EventOverrideDto(
                this.originalStartTime,
                this.title,
                this.content,
                this.startTime,
                this.endTime,
                this.isPrivate
        );
    }

}
