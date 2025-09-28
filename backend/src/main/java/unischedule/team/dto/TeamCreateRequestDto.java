package unischedule.team.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record TeamCreateRequestDto(
    @NotBlank
    @JsonProperty("team_name")
    String teamName,
    
    @JsonProperty("team_description")
    String teamDescription
) {

}
