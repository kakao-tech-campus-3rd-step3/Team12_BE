package unischedule.team.chat.dto;

import jakarta.validation.constraints.Positive;

public record ChatMessageHistoryRequestDto(
        @Positive(message = "cursor는 양수여야 합니다")
        Long cursor
) {
}
