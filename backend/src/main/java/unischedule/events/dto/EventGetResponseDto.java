package unischedule.events.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;

import java.time.LocalDateTime;

public record EventGetResponseDto(
        // 반복 일정의 경우 원본 event ID
        @JsonProperty("event_id")
        Long eventId,
        String title,
        String description,
        @JsonProperty("start_time")
        LocalDateTime startTime,
        @JsonProperty("end_time")
        LocalDateTime endTime,
        @JsonProperty("is_private")
        Boolean isPrivate,
        @JsonProperty("is_recurring")
        Boolean isRecurring
) {
    public static EventGetResponseDto fromSingleEvent(Event event) {
        return new EventGetResponseDto(
                event.getEventId(),
                event.getTitle(),
                event.getContent(),
                event.getStartAt(),
                event.getEndAt(),
                event.getIsPrivate(),
                false
        );
    }

    public static EventGetResponseDto fromRecurringEvent(Event event) {
        return new EventGetResponseDto(
                event.getEventId(),
                event.getTitle(),
                event.getContent(),
                event.getStartAt(),
                event.getEndAt(),
                event.getIsPrivate(),
                true
        );
    }

    public static EventGetResponseDto fromEventOverride(EventOverride eventOverride, Event event) {
        return new EventGetResponseDto(
                event.getEventId(),
                eventOverride.getTitle(),
                eventOverride.getContent(),
                eventOverride.getStartAt(),
                eventOverride.getEndAt(),
                eventOverride.getIsPrivate(),
                true
        );
    }

    public static EventGetResponseDto fromServiceDto(EventServiceDto serviceDto) {
        return new EventGetResponseDto(
                serviceDto.eventId(),
                serviceDto.title(),
                serviceDto.content(),
                serviceDto.startAt(),
                serviceDto.endAt(),
                serviceDto.isPrivate(),
                serviceDto.fromRecurring()
        );
    }
}
