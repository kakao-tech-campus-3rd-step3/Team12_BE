package unischedule.events.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import unischedule.calendar.entity.Calendar;
import unischedule.common.entity.BaseEntity;
import unischedule.exception.InvalidInputException;
import unischedule.member.domain.Member;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="events")
@Getter
public class Event extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="event_id")
    private Long eventId;
    
    @Column(nullable = false)
    private String title;

    private String content;
    
    @Column(nullable = false)
    private LocalDateTime startAt;
    
    @Column(nullable = false)
    private LocalDateTime endAt;
    
    @Column(nullable = false)
    private Boolean isPrivate;

    /**
     * 일정 참여자 범위
     * false, null : 멤버 전체
     * true : EventParticipant 등록된 멤버만 참여
     */
    @Column
    private Boolean isSelective;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "recurrence_rule_id", nullable = true)
    private RecurrenceRule recurrenceRule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_id", nullable = false)
    private Calendar calendar;
    
    @Builder
    public Event(
            String title,
            String content,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Boolean isPrivate,
            Boolean isSelective
    ) {
        this.title = title;
        this.content = content;
        this.startAt = startAt;
        this.endAt = endAt;
        this.isPrivate = isPrivate;
        this.isSelective = (isSelective != null) ? isSelective : false;
    }
    
    public void modifyEvent(String title, String content, LocalDateTime startAt, LocalDateTime endAt, Boolean isPrivate) {
        if(title != null) modifyTitle(title);
        if(content != null) modifyContent(content);
        if(startAt != null) modifyStartAt(startAt);
        if(endAt != null) modifyEndAt(endAt);
        if(isPrivate != null) modifyPrivate(isPrivate);
    }
    
    private void modifyTitle(String title) {
        this.title = title;
    }
    
    private void modifyContent(String content) {
        this.content = content;
    }
    
    private void modifyStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }
    
    private void modifyEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }
    
    private void modifyPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public void updateIsSelective(Boolean isSelective) {
        this.isSelective = isSelective;
    }

    public void connectCalendar(Calendar calendar) {
        this.calendar = calendar;
    }

    public void connectRecurrenceRule(RecurrenceRule recurrenceRule) {
        if (recurrenceRule != null) {
            this.recurrenceRule = recurrenceRule;
        }
    }

    public void validateEventOwner(Member member) {
        this.calendar.validateOwner(member);
    }

    public void validateIsTeamEvent() {
        if (!this.calendar.hasTeam()) {
            throw new InvalidInputException("팀 일정이 아닙니다.");
        }
    }

    public boolean isForAllMembers() {
        return isSelective == null || !isSelective;
    }
}
