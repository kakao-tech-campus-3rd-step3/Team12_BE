package unischedule.team.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import unischedule.team.chat.constant.ChatConstants;
import unischedule.team.chat.dto.ChatMessageDto;

@Service
@RequiredArgsConstructor
public class MessageBrokerService {

    private final RedisTemplate<String, ChatMessageDto> redisTemplate;

    public void broadcastMessage(Long teamId, ChatMessageDto message) {
        try {
            String channel = ChatConstants.getRedisChannel(teamId);
            redisTemplate.convertAndSend(channel, message);
        } catch (Exception ignored) {
        }
    }
}
