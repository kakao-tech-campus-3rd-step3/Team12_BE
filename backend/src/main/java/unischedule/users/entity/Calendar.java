package unischedule.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name="calendars")
public class Calendar {
    @Id
    @Column(name="calendar_id")
    private Long id;
    
    @Column(name="owner_id")
    private Long ownerId;
    
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
}
