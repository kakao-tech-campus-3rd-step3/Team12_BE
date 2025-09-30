package unischedule.team.dto;

import jakarta.validation.constraints.NotBlank;

public record TeamJoinRequestDto(
    @NotBlank
    String visitCode
) {

}
