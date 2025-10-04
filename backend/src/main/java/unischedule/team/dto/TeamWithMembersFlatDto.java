package unischedule.team.dto;

import unischedule.team.domain.TeamRole;

public record TeamWithMembersFlatDto(
        Long teamId,
        String teamName,
        String description,
        Long memberId,
        String memberName,
        TeamRole role
) {
}
