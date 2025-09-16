package unischedule.calendar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import unischedule.common.entity.BaseEntity;
import unischedule.events.entity.Event;
import unischedule.member.entity.Member;

@Entity
@Table(name="calendars")
public class Calendar extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long calendarId;
    
    @OneToOne
    private Member owner;

    private Long teamId;

    @Column(nullable = false, length = 255)
    private String title;

    private String description;
    
    // 우선 개인에 맞춰 작성하면서 넣은것, 추후 삭제 필요
    @OneToMany(mappedBy = "calendar")
    private List<Event> events = new ArrayList<>();
}
