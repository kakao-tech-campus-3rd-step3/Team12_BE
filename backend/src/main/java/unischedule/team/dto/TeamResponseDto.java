package unischedule.team.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TeamResponseDto(
        Long id,
        @JsonProperty("team_name")
        String teamName,
        List<MemberNameResponseDto> members,
        @JsonProperty("member_count")
        Long memberCount,
        @JsonProperty("event_count")
        Long eventCount,
        @JsonProperty("invite_code")
        String inviteCode
) {
}
