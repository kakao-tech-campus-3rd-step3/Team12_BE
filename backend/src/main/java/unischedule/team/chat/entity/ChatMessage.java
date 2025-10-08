package unischedule.team.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import unischedule.common.entity.BaseEntity;

@Entity
@Getter
@Table(
    name = "chat_messages",
    indexes = {
        @Index(name = "idx_team_id", columnList = "team_id"),
        @Index(name = "idx_team_id_created_at", columnList = "team_id, created_at")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;


    @Builder
    public ChatMessage(Long teamId, Long senderId, String senderName, String content) {
        this.teamId = teamId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
    }
}
