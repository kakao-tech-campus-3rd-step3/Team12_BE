package unischedule.users.entity;

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
import unischedule.member.entity.Member;

@Entity
@Table(name="calendars")
public class Calendar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="calendar_id")
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "owner_id")
    private Member owner;
    
    @Column(name="team_id")
    private Long teamId;
    
    @Column(name="summary")
    private String summary;
    
    @Column(name="description")
    private String description;
    
    @Column(name="created_at")
    private LocalDateTime createdAt;
    
    @Column(name="updated_at")
    private LocalDateTime updatedAt;
    
    // 우선 개인에 맞춰 작성하면서 넣은것, 추후 삭제 필요
    @OneToMany(mappedBy = "calendar")
    private List<Event> events = new ArrayList<>();
}
