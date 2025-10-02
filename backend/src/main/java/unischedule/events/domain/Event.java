package unischedule.events.domain;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import unischedule.common.entity.BaseEntity;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.calendar.entity.Calendar;
import unischedule.exception.InvalidInputException;
import unischedule.member.domain.Member;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventState state;
    
    @Column(nullable = false)
    private Boolean isPrivate;
    
    @Column(name = "recurrence_rule_id")
    private Long recurrenceRuleId;
    
    //우선 개인에 맞춰 세팅, 추후 수정 필요
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_id", nullable = false)
    private Calendar calendar;
    
    @Builder
    public Event(
            String title,
            String content,
            LocalDateTime startAt,
            LocalDateTime endAt,
            EventState state,
            Boolean isPrivate
    ) {
        this.title = title;
        this.content = content;
        this.startAt = startAt;
        this.endAt = endAt;
        this.state = state;
        this.isPrivate = isPrivate;
    }
    
    public void modifyEvent(EventModifyRequestDto requestDto) {
        if(requestDto.title() != null) modifyTitle(requestDto.title());
        if(requestDto.description() != null) modifyContent(requestDto.description());
        if(requestDto.startTime() != null) modifyStartAt(requestDto.startTime());
        if(requestDto.endTime() != null) modifyEndAt(requestDto.endTime());
        if(requestDto.isPrivate() != null) modifyPrivate(requestDto.isPrivate());
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

    public void connectCalendar(Calendar calendar) {
        this.calendar = calendar;
    }

    public void validateEventOwner(Member member) {
        this.calendar.validateOwner(member);
    }

    public void validateIsTeamEvent() {
        if (this.calendar.getTeam() == null) {
            throw new InvalidInputException("팀 일정이 아닙니다.");
        }
    }
}
