package unischedule.lecture.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import unischedule.common.entity.BaseEntity;
import unischedule.events.domain.Event;

import java.time.LocalDate;

@Entity
@Table(name = "lectures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lecture extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lectureId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String professor;

    private Integer credit;
    
    @Column(nullable = false)
    private LocalDate startDate;
    
    @Column(nullable = false)
    private LocalDate endDate;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    
    @Builder
    public Lecture(String name, String professor, Integer credit, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.professor = professor;
        this.credit = credit;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    public void connectEvent(Event event) {
        this.event = event;
    }
}

