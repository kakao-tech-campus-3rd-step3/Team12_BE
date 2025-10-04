package unischedule.team.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.GeneratedValue;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
public class WhenToMeetResponseDto {
    
    @JsonProperty("start_time")
    private final LocalDateTime startTime;
    
    @JsonProperty("end_time")
    private final LocalDateTime endTime;
    
    @JsonProperty("available_member")
    private Long availableMember;
    
    public void discountAvailable() {
        this.availableMember--;
    }
}

