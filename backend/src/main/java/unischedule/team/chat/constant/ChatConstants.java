package unischedule.team.chat.constant;

public final class ChatConstants {

    public static final String REDIS_CHANNEL_PREFIX = "team:chat:";
    
    public static final String MESSAGE_TYPE_SEND = "SEND_MESSAGE";
    public static final String MESSAGE_TYPE_NEW = "NEW_MESSAGE";
    public static final String MESSAGE_TYPE_ERROR = "ERROR";

    public static String getRedisChannel(Long teamId) {
        return REDIS_CHANNEL_PREFIX + teamId;
    }

    public static Long extractTeamIdFromChannel(String channel) {
        if (channel != null && channel.startsWith(REDIS_CHANNEL_PREFIX)) {
            try {
                String teamIdStr = channel.substring(REDIS_CHANNEL_PREFIX.length());
                return Long.parseLong(teamIdStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}

