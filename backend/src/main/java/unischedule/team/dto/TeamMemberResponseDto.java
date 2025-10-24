package unischedule.team.dto;

import unischedule.team.domain.TeamMember;
import unischedule.team.domain.TeamRole;

public record TeamMemberResponseDto(
        Long id,
        TeamRole role,
        String name
) {
    public static TeamMemberResponseDto from(TeamMember teamMember) {
        return new TeamMemberResponseDto(
                teamMember.getMember().getMemberId(),
                teamMember.getRole(),
                teamMember.getMember().getNickname()
        );
    }
}
