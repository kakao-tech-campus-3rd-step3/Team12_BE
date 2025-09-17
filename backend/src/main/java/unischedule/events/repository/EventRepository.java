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
    /**
     * 사용자의 특정 기간동안의 모든 일정 조회
     * @param calendarIds
     * @param startAt
     * @param endAt
     * @return
     */
    @Query("""
            SELECT e
            FROM Event e
            WHERE e.calendar.calendarId IN :calendarIds
            AND e.endAt >= :startAt
            AND e.startAt <= :endAt
    """)
    List<Event> findScheduleInPeriod(
            @Param("calendarIds")
            List<Long> calendarIds,
            @Param("startAt")
            LocalDateTime startAt,
            @Param("endAt")
            LocalDateTime endAt
    );

    /**
     * 사용자의 특정 기간동안의 일정 존재 여부 확인
     * @param calendarIds
     * @param startAt
     * @param endAt
     * @return
     */
    @Query("""
            SELECT count(e) > 0
            FROM Event e
            WHERE e.calendar.calendarId IN :calendarIds
            AND e.endAt >= :startAt
            AND e.startAt <= :endAt
    """)
    boolean existsScheduleInPeriod(
            @Param("calendarIds")
            List<Long> calendarIds,
            @Param("startAt")
            LocalDateTime startAt,
            @Param("endAt")
            LocalDateTime endAt
    );

    /**
     * 특정 이벤트를 제외하고 시간 중복 확인 (일정 수정 시 사용)
     * @param calendarIds
     * @param startAt
     * @param endAt
     * @param eventId
     * @return
     */
    @Query("""
            SELECT count(e) > 0
            FROM Event e
            WHERE e.calendar.calendarId IN :calendarIds
            AND e.eventId != :eventId
            AND e.endAt > :startAt
            AND e.startAt < :endAt
    """)
    boolean existsScheduleInPeriodExcludingEvent(
            @Param("calendarIds")
            List<Long> calendarIds,
            @Param("startAt")
            LocalDateTime startAt,
            @Param("endAt")
            LocalDateTime endAt,
            @Param("eventId")
            Long eventId
    );
}
