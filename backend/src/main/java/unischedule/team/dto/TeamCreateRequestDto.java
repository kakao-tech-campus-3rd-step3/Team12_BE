package unischedule.team.dto;

import jakarta.validation.constraints.NotBlank;

public record TeamCreateRequestDto(
    @NotBlank
    String name
) {

}
