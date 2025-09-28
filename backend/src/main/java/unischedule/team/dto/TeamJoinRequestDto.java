package unischedule.team.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record TeamJoinRequestDto(
    @NotBlank
    @JsonProperty("invite_code")
    String inviteCode
) {

}
