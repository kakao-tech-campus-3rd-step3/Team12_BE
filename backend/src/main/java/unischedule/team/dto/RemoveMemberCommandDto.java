package unischedule.team.dto;

public record RemoveMemberCommandDto(
        String leaderEmail,
        Long teamId,
        Long targetMemberId
) {
}
