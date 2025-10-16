package unischedule.events.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import unischedule.events.dto.EventExceptionDto;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_exception_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "original_event_id", nullable = false)
    private Event originalEvent;

    @Column(nullable = false)
    private LocalDateTime originalEventTime;

    @Column(nullable = true)
    private String title;
    @Column(nullable = true)
    private String content;
    @Column(nullable = true)
    private LocalDateTime startAt;
    @Column(nullable = true)
    private LocalDateTime endAt;
    @Column(nullable = true)
    private Boolean isPrivate;

    public EventException(
            Event originalEvent,
            LocalDateTime originalEventTime,
            String title,
            String content,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Boolean isPrivate
    ) {
        this.originalEvent = originalEvent;
        this.originalEventTime = originalEventTime;
        this.title = title;
        this.content = content;
        this.startAt = startAt;
        this.endAt = endAt;
        this.isPrivate = isPrivate;
    }

    public static EventException makeEventException(Event originEvent, EventExceptionDto exceptionDto) {
        return new EventException(
                originEvent,
                exceptionDto.originalStartTime(),
                getValueOrDefault(exceptionDto.title(), originEvent.getTitle()),
                getValueOrDefault(exceptionDto.content(), originEvent.getContent()),
                getValueOrDefault(exceptionDto.startTime(), originEvent.getStartAt()),
                getValueOrDefault(exceptionDto.endTime(), originEvent.getEndAt()),
                getValueOrDefault(exceptionDto.isPrivate(), originEvent.getIsPrivate())
        );
    }

    public static EventException makeEventDeleteException(Event originEvent, LocalDateTime originStartTime) {
        return new EventException(
                originEvent,
                originStartTime,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static <T> T getValueOrDefault(T value, T defaultValue) {
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    public void update(
            LocalDateTime originalEventTime,
            String title,
            String content,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Boolean isPrivate
    ) {
        updateOriginalEventTime(originalEventTime);
        updateTitle(title);
        updateContent(content);
        updateStartAt(startAt);
        updateEndAt(endAt);
        updateIsPrivate(isPrivate);
    }

    private void updateOriginalEventTime(LocalDateTime originalEventTime) {
        if (originalEventTime != null) {
            this.originalEventTime = originalEventTime;
        }
    }

    private void updateTitle(String title) {
        if (title != null && title.isBlank()) {
            this.title = title;
        }
    }

    private void updateContent(String content) {
        if (content != null && title.isBlank()) {
            this.content = content;
        }
    }

    private void updateStartAt(LocalDateTime startAt) {
        if (startAt != null) {
            this.startAt = startAt;
        }
    }

    private void updateEndAt(LocalDateTime endAt) {
        if (endAt != null) {
            this.endAt = endAt;
        }
    }

    private void updateIsPrivate(Boolean isPrivate) {
        if (isPrivate != null) {
            this.isPrivate = isPrivate;
        }
    }

    public Event toEvent() {
        return new Event(
                this.title,
                this.content,
                this.startAt,
                this.endAt,
                EventState.CONFIRMED,
                this.isPrivate
        );
    }
}
