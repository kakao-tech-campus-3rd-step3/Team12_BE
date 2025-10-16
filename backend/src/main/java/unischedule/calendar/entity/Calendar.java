package unischedule.calendar.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import org.springframework.security.access.AccessDeniedException;
import unischedule.common.entity.BaseEntity;
import unischedule.member.domain.Member;
import unischedule.team.domain.Team;

@Entity
@Getter
@Table(name = "calendars")
public class Calendar extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long calendarId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_calendar_member_id_ref_member_id")
    )
    private Member owner;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "team_id",
            foreignKey = @ForeignKey(name = "fk_calendar_team_id_ref_team_id")
    )
    private Team team;

    protected Calendar() {

    }

    public Calendar(Member owner, Team team) {
        this.owner = owner;
        this.team = team;
    }

    public Calendar(Member owner) {
        this(owner, null);
    }

    public void validateOwner(Member member) {
        if (!this.owner.isEqualMember(member)) {
            throw new AccessDeniedException("해당 캘린더에 대한 접근 권한이 없습니다.");
        }
    }

    public boolean hasTeam() {
        return this.team != null;
    }
}
