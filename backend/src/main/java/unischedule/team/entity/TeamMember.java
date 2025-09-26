package unischedule.team.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import unischedule.exception.NoPermissionException;
import unischedule.member.entity.Member;

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
    
    @Column(nullable = false)
    private String role; // 예: LEADER, MEMBER 등
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    public TeamMember(Team team, Member member, String role) {
        this.team = team;
        this.member = member;
        this.role = role;
    }
    
    public void checkLeader() {
        if(!this.role.equals("LEADER")) {
            throw new NoPermissionException("리더가 아닙니다.");
        }
    }
}
