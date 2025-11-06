package unischedule.events.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record TeamEventGetResponseDto(
        @JsonProperty("event_id")
        Long eventId,
        String title,
        String description,
        @JsonProperty("start_time")
        LocalDateTime startTime,
        @JsonProperty("end_time")
        LocalDateTime endTime,
        @JsonProperty("is_recurring")
        Boolean isRecurring,
        @JsonProperty("event_participants")
        List<Long> eventParticipants
) {
    public static TeamEventGetResponseDto from(EventServiceDto serviceDto, List<Long> participantIds) {
        return new TeamEventGetResponseDto(
                serviceDto.eventId(),
                serviceDto.title(),
                serviceDto.content(),
                serviceDto.startAt(),
                serviceDto.endAt(),
                serviceDto.fromRecurring(),
                participantIds
        );
    }
}
