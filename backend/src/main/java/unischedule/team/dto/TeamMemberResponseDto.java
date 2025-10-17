package unischedule.team.dto;

import unischedule.team.domain.TeamRole;

public record TeamMemberResponseDto(
        Long id,
        TeamRole role,
        String name
) {
}
