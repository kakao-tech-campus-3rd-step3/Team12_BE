package unischedule.team.chat.util;

import java.net.URI;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ChatWebSocketUrlParser {

    private static final String TOKEN_PARAM = "token";

    public Long extractTeamId(WebSocketSession session) {
        UriComponents components = buildComponents(session);
        if (components == null) {
            return null;
        }

        List<String> segments = components.getPathSegments();
        if (segments.size() < 4) {
            return null;
        }
        return parseTeamId(segments.get(2));
    }

    public String extractToken(WebSocketSession session) {
        UriComponents components = buildComponents(session);
        if (components == null) {
            return null;
        }
        return components.getQueryParams().getFirst(TOKEN_PARAM);
    }

    private UriComponents buildComponents(WebSocketSession session) {
        URI uri = (session != null) ? session.getUri() : null;
        if (uri == null) {
            return null;
        }
        try {
            return UriComponentsBuilder.fromUri(uri).build();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Long parseTeamId(String candidate) {
        try {
            return Long.parseLong(candidate);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
