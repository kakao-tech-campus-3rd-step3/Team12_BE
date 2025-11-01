package unischedule.team.dto;

import unischedule.team.domain.Team;

public record TeamDetailResponseDto(
        Long id,
        String name,
        String description,
        int count,
        String code
) {
    public static TeamDetailResponseDto of(Team team, int memberCount) {
        return new TeamDetailResponseDto(
                team.getTeamId(),
                team.getName(),
                team.getDescription(),
                memberCount,
                team.getInviteCode()
        );
    }
}
