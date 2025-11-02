package unischedule.team.chat.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import unischedule.auth.jwt.JwtTokenProvider;
import unischedule.common.config.SecurityConfig;
import unischedule.google.handler.OAuth2LoginSuccessHandler;
import unischedule.team.chat.dto.ChatMessageDto;
import unischedule.team.chat.dto.ChatMessageHistoryResponseDto;
import unischedule.team.chat.service.ChatService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import(SecurityConfig.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;
    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Test
    @DisplayName("커서 없이 메시지 조회 - 최신 메시지 반환")
    void getMessagesWithoutCursor() throws Exception {
        ChatMessageDto message = ChatMessageDto.builder()
                .id(1L)
                .teamId(1L)
                .senderId(2L)
                .senderName("홍길동")
                .content("안녕하세요")
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessageHistoryResponseDto response = new ChatMessageHistoryResponseDto(
                List.of(message),
                true,
                1L
        );

        when(chatService.getMessages(1L, "user@example.com", null, 50))
                .thenReturn(response);

        mockMvc.perform(get("/api/teams/{teamId}/chat/messages", 1L)
                        .with(user("user@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].id").value(1L))
                .andExpect(jsonPath("$.messages[0].content").value("안녕하세요"))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").value(1L));
    }

    @Test
    @DisplayName("커서로 메시지 조회 - 이전 메시지 반환")
    void getMessagesWithCursor() throws Exception {
        ChatMessageDto message = ChatMessageDto.builder()
                .id(5L)
                .teamId(1L)
                .senderId(3L)
                .senderName("김철수")
                .content("이전 메시지")
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessageHistoryResponseDto response = new ChatMessageHistoryResponseDto(
                List.of(message),
                false,
                null
        );

        when(chatService.getMessages(1L, "user@example.com", 10L, 50))
                .thenReturn(response);

        mockMvc.perform(get("/api/teams/{teamId}/chat/messages", 1L)
                        .param("cursor", "10")
                        .with(user("user@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].id").value(5L))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }
}
