package unischedule.team.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class WhenToMeet {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long availableMember;
    
    public void discountAvailable() {
        this.availableMember--;
    }
}
