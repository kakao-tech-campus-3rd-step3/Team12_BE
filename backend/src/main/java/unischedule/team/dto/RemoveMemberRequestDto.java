package unischedule.team.dto;

public record RemoveMemberRequestDto(
        String leaderEmail,
        Long teamId,
        Long memberId
) {
}
