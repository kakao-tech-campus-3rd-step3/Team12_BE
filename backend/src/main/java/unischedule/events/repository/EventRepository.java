package unischedule.events.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import unischedule.events.entity.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByStartAtLessThanAndEndAtGreaterThan(
        LocalDateTime endAt,
        LocalDateTime startAt
    );

    @Query("""
            SELECT e
            FROM Event e
            WHERE e.calendar.calendarId = :calendarId
            AND e.endAt >= :startAt
            AND e.startAt <= :endAt
    """)
    List<Event> findScheduleInPeriod(
            @Param("calendarId")
            Long calendarId,
            @Param("startAt")
            LocalDateTime startAt,
            @Param("endAt")
            LocalDateTime endAt
    );
    
    boolean existsByStartAtLessThanAndEndAtGreaterThan(
        LocalDateTime endAt,
        LocalDateTime startAt
    );

    @Query("""
            SELECT count(e) > 0
            FROM Event e
            WHERE e.calendar.calendarId = :calendarId
            AND e.endAt >= :startAt
            AND e.startAt <= :endAt
    """)
    boolean existsScheduleInPeriod(
            @Param("calendarId")
            Long calendarId,
            @Param("startAt")
            LocalDateTime startAt,
            @Param("endAt")
            LocalDateTime endAt
    );
}
