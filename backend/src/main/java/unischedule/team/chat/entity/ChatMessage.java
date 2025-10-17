package unischedule.team.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import unischedule.common.entity.BaseEntity;
import unischedule.member.domain.Member;
import unischedule.team.domain.Team;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Member sender;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;


    @Builder
    public ChatMessage(Team team, Member sender, String senderName, String content) {
        this.team = team;
        this.sender = sender;
        this.senderName = senderName;
        this.content = content;
    }
}
