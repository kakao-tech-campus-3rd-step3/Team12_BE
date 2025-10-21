package unischedule.team.chat.dto;

import java.util.List;

public record ChatMessageHistoryResponseDto(
        List<ChatMessageDto> messages,
        boolean hasNext,
        Long nextCursor
) {
    public ChatMessageHistoryResponseDto {
        messages = List.copyOf(messages);
    }
}
