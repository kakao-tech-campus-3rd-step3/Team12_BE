package unischedule.team.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TeamResponseDto(
        Long id,
        @JsonProperty("team_name")
        String teamName,
        List<MemberNameResponseDto> members,
        @JsonProperty("member_count")
        int memberCount,
        @JsonProperty("invite_code")
        String inviteCode
) {
}
