package unischedule.team.chat.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import unischedule.team.chat.constant.ChatConstants;
import unischedule.team.chat.dto.ChatMessageDto;
import unischedule.team.chat.handler.ChatWebSocketHandler;

@Component
@RequiredArgsConstructor
public class ChatMessageListener implements MessageListener {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());
            
            Long teamId = ChatConstants.extractTeamIdFromChannel(channel);
            if (teamId != null) {
                ChatMessageDto chatMessage = objectMapper.readValue(body, ChatMessageDto.class);
                chatWebSocketHandler.broadcastToTeam(teamId, chatMessage);
            }
        } catch (Exception ignored) {
        }
    }
}
