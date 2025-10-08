package unischedule.team.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequestDto(
        @NotBlank(message = "메시지 내용은 필수입니다")
        String content
) {
}
