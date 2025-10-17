package unischedule.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import unischedule.team.chat.dto.ChatMessageDto;
import unischedule.team.chat.listener.ChatMessageListener;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ChatMessageDto> redisTemplate(
            RedisConnectionFactory connectionFactory, 
            ObjectMapper objectMapper) {
        RedisTemplate<String, ChatMessageDto> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        Jackson2JsonRedisSerializer<ChatMessageDto> serializer = 
                new Jackson2JsonRedisSerializer<>(objectMapper, ChatMessageDto.class);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ChatMessageListener chatMessageListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(chatMessageListener, new PatternTopic("team:chat:*"));

        return container;
    }
}
