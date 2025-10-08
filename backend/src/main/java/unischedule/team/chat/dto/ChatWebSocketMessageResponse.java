package unischedule.team.chat.dto;

public record ChatWebSocketMessageResponse(
        String type,
        ChatMessageDto data
) {
}

