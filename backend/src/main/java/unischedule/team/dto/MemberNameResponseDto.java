package unischedule.team.dto;

import unischedule.team.domain.TeamMember;

public record MemberNameResponseDto(
        String name
) {
    public static MemberNameResponseDto from(TeamMember teamMember) {
        return new MemberNameResponseDto(teamMember.getMember().getNickname());
    }
}
