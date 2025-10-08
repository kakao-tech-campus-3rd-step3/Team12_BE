package unischedule.team.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import unischedule.auth.jwt.JwtTokenProvider;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.chat.constant.ChatConstants;
import unischedule.team.domain.Team;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.team.service.internal.TeamRawService;
import unischedule.team.chat.dto.ChatMessageDto;
import unischedule.team.chat.dto.ChatMessageRequestDto;
import unischedule.team.chat.dto.ChatWebSocketRequestDto;
import unischedule.team.chat.dto.ChatWebSocketMessageResponse;
import unischedule.team.chat.dto.ChatWebSocketErrorResponse;
import unischedule.team.chat.service.ChatService;
import unischedule.team.chat.service.MessageBrokerService;
import unischedule.team.chat.util.ChatWebSocketSessionManager;
import unischedule.team.chat.util.ChatWebSocketUrlParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private static final String ATTR_TEAM_ID = "teamId";
    private static final String ATTR_USER_EMAIL = "userEmail";

    private final ChatService chatService;
    private final MessageBrokerService messageBrokerService;
    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final ChatWebSocketSessionManager sessionManager;
    private final ChatWebSocketUrlParser urlParser;
    private final MemberRawService memberRawService;
    private final TeamRawService teamRawService;
    private final TeamMemberRawService teamMemberRawService;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        Long teamId = urlParser.extractTeamId(session);
        String token = urlParser.extractToken(session);

        if (!isValidHandshake(teamId, token)) {
            sendErrorResponse(session, "올바르지 않은 연결 정보입니다");
            session.close();
            return;
        }

        String userEmail = jwtTokenProvider.getEmail(token);
        session.getAttributes().put(ATTR_TEAM_ID, teamId);
        session.getAttributes().put(ATTR_USER_EMAIL, userEmail);
        sessionManager.addSession(teamId, session);
    }

    @Override
    public void handleMessage(
            @NonNull WebSocketSession session,
            @NonNull WebSocketMessage<?> message) throws Exception {
        Long teamId = resolveTeamId(session);
        if (teamId == null) {
            sendErrorResponse(session, "올바르지 않은 연결 정보입니다");
            return;
        }

        ChatWebSocketRequestDto request = parseRequest(message, session);
        if (request == null || !ChatConstants.MESSAGE_TYPE_SEND.equals(request.type())) {
            return;
        }

        String userEmail = resolveUserEmail(session);
        if (userEmail == null) {
            sendErrorResponse(session, "인증이 필요합니다");
            return;
        }

        try {
            ChatMessageDto messageDto = chatService.sendMessage(
                    teamId, userEmail, new ChatMessageRequestDto(request.content()));
            messageBrokerService.broadcastMessage(teamId, messageDto);
        } catch (Exception ignored) {
            sendErrorResponse(session, "메시지 전송에 실패했습니다");
        }
    }

    @Override
    public void handleTransportError(
            @NonNull WebSocketSession session,
            @NonNull Throwable exception) throws Exception {
        sendErrorResponse(session, "통신 중 오류가 발생했습니다");
    }

    @Override
    public void afterConnectionClosed(
            @NonNull WebSocketSession session,
            @NonNull CloseStatus closeStatus) throws Exception {
        Long teamId = urlParser.extractTeamId(session);
        if (teamId != null) {
            sessionManager.removeSession(teamId, session.getId());
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public void broadcastToTeam(Long teamId, ChatMessageDto message) {
        Map<String, WebSocketSession> sessions = sessionManager.getTeamSessions(teamId);
        if (sessions.isEmpty()) {
            return;
        }

        String messageJson = serializeBroadcastMessage(message);
        if (messageJson == null) {
            return;
        }

        List<String> closedSessionIds = new ArrayList<>();
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            if (!sendToSession(entry.getValue(), messageJson)) {
                closedSessionIds.add(entry.getKey());
            }
        }

        if (!closedSessionIds.isEmpty()) {
            sessionManager.cleanupClosedSessions(teamId, closedSessionIds);
        }
    }

    private boolean isValidHandshake(Long teamId, String token) {
        if (token == null || teamId == null) {
            return false;
        }

        if (!jwtTokenProvider.validateToken(token)) {
            return false;
        }
        
        String email = jwtTokenProvider.getEmail(token);
        if (email == null) {
            return false;
        }
        
        return validateTeamMembership(teamId, email);
    }

    private Long resolveTeamId(WebSocketSession session) {
        Object attribute = session.getAttributes().get(ATTR_TEAM_ID);
        if (attribute instanceof Long cachedTeamId) {
            return cachedTeamId;
        }
        Long teamId = urlParser.extractTeamId(session);
        if (teamId != null) {
            session.getAttributes().put(ATTR_TEAM_ID, teamId);
        }
        return teamId;
    }

    private String resolveUserEmail(WebSocketSession session) {
        Object attribute = session.getAttributes().get(ATTR_USER_EMAIL);
        if (attribute instanceof String cachedEmail) {
            return cachedEmail;
        }

        String token = urlParser.extractToken(session);
        if (!isValidToken(token)) {
            return null;
        }

        String email = jwtTokenProvider.getEmail(token);
        if (email != null) {
            session.getAttributes().put(ATTR_USER_EMAIL, email);
        }
        return email;
    }

    private ChatWebSocketRequestDto parseRequest(WebSocketMessage<?> message, WebSocketSession session) {
        if (!(message instanceof TextMessage textMessage)) {
            sendErrorResponse(session, "지원하지 않는 메시지 형식입니다");
            return null;
        }

        try {
            return objectMapper.readValue(
                    textMessage.getPayload(), ChatWebSocketRequestDto.class);
        } catch (IOException e) {
            sendErrorResponse(session, "잘못된 메시지 형식입니다");
            return null;
        }
    }

    private String serializeBroadcastMessage(ChatMessageDto message) {
        try {
            return objectMapper.writeValueAsString(
                    new ChatWebSocketMessageResponse(ChatConstants.MESSAGE_TYPE_NEW, message));
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean sendToSession(WebSocketSession session, String payload) {
        if (!session.isOpen()) {
            return false;
        }
        try {
            session.sendMessage(new TextMessage(payload));
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private void sendErrorResponse(WebSocketSession session, String errorMessage) {
        if (!session.isOpen()) {
            return;
        }

        try {
            String errorJson = objectMapper.writeValueAsString(
                    new ChatWebSocketErrorResponse(ChatConstants.MESSAGE_TYPE_ERROR, errorMessage));
            session.sendMessage(new TextMessage(errorJson));
        } catch (IOException ignored) {
        }
    }

    private boolean validateTeamMembership(Long teamId, String email) {
        if (teamId == null || email == null) {
            return false;
        }
        try {
            Team team = teamRawService.findTeamById(teamId);
            Member member = memberRawService.findMemberByEmail(email);
            teamMemberRawService.validateMembership(team, member);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isValidToken(String token) {
        return token != null && jwtTokenProvider.validateToken(token);
    }
}
