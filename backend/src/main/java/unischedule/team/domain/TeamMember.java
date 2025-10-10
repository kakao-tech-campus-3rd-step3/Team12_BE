package unischedule.team.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import unischedule.exception.NoPermissionException;
import unischedule.member.domain.Member;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "team_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamRole role; // 예: LEADER, MEMBER 등
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    public TeamMember(Team team, Member member, TeamRole role) {
        this.team = team;
        this.member = member;
        this.role = role;
    }
    
    public void checkLeader() {
        if(!this.role.equals(TeamRole.LEADER)) {
            throw new NoPermissionException("리더가 아닙니다.");
        }
    }

    public void validateRemovable() {
        if (this.role.equals(TeamRole.LEADER)) {
            throw new NoPermissionException("팀장 권한을 가진 멤버는 제거할 수 없습니다.");
        }
    }
}
