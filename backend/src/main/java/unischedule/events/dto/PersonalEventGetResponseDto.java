package unischedule.events.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;

import java.time.LocalDateTime;

public record PersonalEventGetResponseDto (
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
        String type
) {
    public static PersonalEventGetResponseDto fromSingleEvent(Event event, String type) {
        return new PersonalEventGetResponseDto(
                event.getEventId(),
                event.getTitle(),
                event.getContent(),
                event.getStartAt(),
                event.getEndAt(),
                false,
                type
        );
    }

    public static PersonalEventGetResponseDto fromRecurringEvent(Event event, String type) {
        return new PersonalEventGetResponseDto(
                event.getEventId(),
                event.getTitle(),
                event.getContent(),
                event.getStartAt(),
                event.getEndAt(),
                true,
                type
        );
    }

    public static PersonalEventGetResponseDto fromEventOverride(EventOverride eventOverride, Event event, String type) {
        return new PersonalEventGetResponseDto(
                event.getEventId(),
                eventOverride.getTitle(),
                eventOverride.getContent(),
                eventOverride.getStartAt(),
                eventOverride.getEndAt(),
                true,
                type
        );
    }

    public static PersonalEventGetResponseDto fromServiceDto(EventServiceDto serviceDto, String type) {
        return new PersonalEventGetResponseDto(
                serviceDto.eventId(),
                serviceDto.title(),
                serviceDto.content(),
                serviceDto.startAt(),
                serviceDto.endAt(),
                serviceDto.fromRecurring(),
                type
        );
    }

}
