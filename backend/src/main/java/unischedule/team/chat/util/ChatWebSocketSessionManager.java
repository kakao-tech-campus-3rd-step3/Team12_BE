package unischedule.team.chat.util;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketSessionManager {

    private final Map<Long, Map<String, WebSocketSession>> teamChatSessions = new ConcurrentHashMap<>();

    public void addSession(Long teamId, WebSocketSession session) {
        teamChatSessions.computeIfAbsent(teamId, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
    }

    public void removeSession(Long teamId, String sessionId) {
        teamChatSessions.computeIfPresent(teamId, (id, sessions) -> {
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public Map<String, WebSocketSession> getTeamSessions(Long teamId) {
        Map<String, WebSocketSession> sessions = teamChatSessions.get(teamId);
        if (sessions == null || sessions.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(sessions);
    }

    public void cleanupClosedSessions(Long teamId, Collection<String> closedSessionIds) {
        if (closedSessionIds == null || closedSessionIds.isEmpty()) {
            return;
        }

        teamChatSessions.computeIfPresent(teamId, (id, sessions) -> {
            sessions.keySet().removeAll(Set.copyOf(closedSessionIds));
            return sessions.isEmpty() ? null : sessions;
        });
    }
}
