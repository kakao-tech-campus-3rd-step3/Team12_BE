package unischedule.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name="events")
public class Event {
    @Id
    @Column(name="event_id")
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "calendar_id")
    private Calendar calendar;
    
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
    
    @Column(name = "recurrence_rule_id")
    private Long recurrenceRuleId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
