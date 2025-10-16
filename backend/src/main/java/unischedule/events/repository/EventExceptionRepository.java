package unischedule.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventExceptionRepository extends JpaRepository<EventException, Long> {

    @Query("""
            SELECT ex
            FROM EventException ex
            WHERE ex.originalEvent = :originalEvent
            AND ex.originalEventTime = :originalEventTime
    """)
    Optional<EventException> findByOriginEventTime(
            @Param("originalEvent")
            Event originalEvent,
            @Param("originalEventTime")
            LocalDateTime originalEventTime
    );

    @Query("""
            SELECT ex
            FROM EventException ex
            WHERE ex.originalEvent IN :originalEvents
            AND ex.originalEventTime >= :startAt
            AND ex.originalEventTime < :endAt
    """)
    List<EventException> findEventExceptionsForEvents(
            @Param("originalEvents")
            List<Event> originalEvents,
            @Param("startAt")
            LocalDateTime startAt,
            @Param("andAt")
            LocalDateTime endAt
    );

    void deleteAllByOriginalEvent(Event event);
}
