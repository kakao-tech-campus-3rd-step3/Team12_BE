package unischedule.google.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleSyncRequestDto(
        @JsonProperty("redirect_url")
        String redirectUrl
) {
}
