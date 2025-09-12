package unischedule.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record AccessTokenRefreshRequestDto(
        @NotBlank
        @JsonProperty("refresh_token")
        String refreshToken
) {
}
