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
            SELECT ex
            FROM EventOverride ex
            WHERE ex.originalEvent = :originalEvent
            AND ex.originalEventTime = :originalEventTime
    """)
    Optional<EventOverride> findByOriginEventTime(
            @Param("originalEvent")
            Event originalEvent,
            @Param("originalEventTime")
            LocalDateTime originalEventTime
    );

    @Query("""
            SELECT ex
            FROM EventOverride ex
            WHERE ex.originalEvent IN :originalEvents
            AND ex.originalEventTime >= :startAt
            AND ex.originalEventTime < :endAt
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
