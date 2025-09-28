package unischedule.team.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TeamJoinResponseDto(
    @JsonProperty("team_id")
    Long teamId,
    @JsonProperty("team_name")
    String teamName,
    @JsonProperty("team_description")
    String teamDescription
) {

}
