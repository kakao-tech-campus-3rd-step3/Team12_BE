package unischedule.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MemberTokenResponseDto(
        @JsonProperty("access_token")
        String accessToken
) {
}
