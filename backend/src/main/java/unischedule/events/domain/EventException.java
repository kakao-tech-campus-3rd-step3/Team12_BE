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

    @Column(nullable = false)
    private String title;
    private String content;
    @Column(nullable = false)
    private LocalDateTime startAt;
    @Column(nullable = false)
    private LocalDateTime endAt;
    @Column(nullable = false)
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

    public void update(
            String title,
            String content,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Boolean isPrivate
    ) {
        updateTitle(title);
        updateContent(content);
        updateStartAt(startAt);
        updateEndAt(endAt);
        updateIsPrivate(isPrivate);
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
}
