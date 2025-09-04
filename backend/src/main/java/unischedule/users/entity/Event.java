package unischedule.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="events")
@Getter
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="event_id")
    private Long id;
    
    @Column(name = "creator_id")
    private Long creatorId;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "content")
    private String content;
    
    @Column(name = "start_at")
    private LocalDateTime startAt;
    
    @Column(name = "end_at")
    private LocalDateTime endAt;
    
    @Column(name = "state")
    private String state;
    
    @Column(name="is_private")
    private Boolean isPrivate;
    
    @Column(name = "recurrence_rule_id")
    private Long recurrenceRuleId;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    //우선 개인에 맞춰 세팅, 추후 수정 필요
    @ManyToOne
    @JoinColumn(name = "calendar_id")
    private Calendar calendar;
    
    @Builder
    public Event(Long creatorId, String title, String content,
        LocalDateTime startAt, LocalDateTime endAt,
        String state, Boolean isPrivate) {
        this.creatorId = creatorId;
        this.title = title;
        this.content = content;
        this.startAt = startAt;
        this.endAt = endAt;
        this.state = state;
        this.isPrivate = isPrivate;
    }
    
    // 테스트 전용
    public Event(Long id, Long creatorId, String title, String content,
        LocalDateTime startAt, LocalDateTime endAt, String state, Boolean isPrivate) {
        this.id = id;
        this.creatorId = creatorId;
        this.title = title;
        this.content = content;
        this.startAt = startAt;
        this.endAt = endAt;
        this.state = state;
        this.isPrivate = isPrivate;
    }
}
