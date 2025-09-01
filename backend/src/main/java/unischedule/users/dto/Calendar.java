package unischedule.users.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="calendars")
public class Calendar {
    @Id
    @Column(name="calendar_id")
    private Long id;
    
    //owner id
    
    //team id
    
    //summary
    
    //description
    
    //created at
    
    //updated at
}
