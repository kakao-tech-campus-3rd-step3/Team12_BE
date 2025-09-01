package unischedule.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="events")
public class Event {
    @Id
    @Column(name="event_id")
    private Long id;
    
    //calendar id
    
    //creator id
    
    //title
    
    //content
    
    //start at
    
    //end at
    
    //state
    
    //recurrence rule id
    
    //created at
    
    //updated at
}
