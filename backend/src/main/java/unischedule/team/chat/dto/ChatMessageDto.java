package unischedule.team.chat.dto;

import lombok.Builder;
import unischedule.team.chat.entity.ChatMessage;

import java.time.LocalDateTime;

@Builder
public record ChatMessageDto(
        Long id,
        Long teamId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime createdAt
) {
    public static ChatMessageDto from(ChatMessage chatMessage) {
        return ChatMessageDto.builder()
                .id(chatMessage.getId())
                .teamId(chatMessage.getTeamId())
                .senderId(chatMessage.getSenderId())
                .senderName(chatMessage.getSenderName())
                .content(chatMessage.getContent())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }
}
