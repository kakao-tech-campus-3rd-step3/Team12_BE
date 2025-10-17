package unischedule.team.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;


public record WhenToMeetResponseDto(@JsonProperty("start_time") LocalDateTime startTime,
                                    @JsonProperty("end_time") LocalDateTime endTime,
                                    @JsonProperty("available_member") Long availableMember) {
    
}

