package unischedule.team.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TeamCreateResponseDto(
    @JsonProperty("team_id")
    Long teamId,
    
    @JsonProperty("team_name")
    String teamName,
    
    @JsonProperty("team_description")
    String teamDescription,
    
    @JsonProperty("team_code")
    String teamCode
) {

}
