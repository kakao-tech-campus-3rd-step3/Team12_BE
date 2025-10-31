package unischedule.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventOverrideRepository extends JpaRepository<EventOverride, Long> {

    @Query("""
            SELECT eo
            FROM EventOverride eo
            WHERE eo.originalEvent = :originalEvent
            AND eo.startAt = :modifiedStartTime
            AND eo.title IS NOT NULL
    """)
    Optional<EventOverride> findByEventStartTime(
            @Param("originalEvent")
            Event originalEvent,
            @Param("modifiedStartTime")
            LocalDateTime modifiedStartTime
    );

    boolean existsByOriginalEventAndOriginalEventTimeAndTitleIsNull(
            Event originalEvent,
            LocalDateTime originalEventTime
    );

    @Query("""
            SELECT eo
            FROM EventOverride eo
            WHERE eo.originalEvent IN :originalEvents
            AND eo.originalEventTime >= :startAt
            AND eo.originalEventTime < :endAt
    """)
    List<EventOverride> findEventOverridesForEvents(
            @Param("originalEvents")
            List<Event> originalEvents,
            @Param("startAt")
            LocalDateTime startAt,
            @Param("endAt")
            LocalDateTime endAt
    );

    void deleteAllByOriginalEvent(Event event);
}
