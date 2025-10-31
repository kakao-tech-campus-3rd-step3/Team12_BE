package unischedule.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record SendEmailResponseDto(
        @JsonProperty("expires_at")
        LocalDateTime expiresAt
) {
}
