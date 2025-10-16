package unischedule.events.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import unischedule.events.domain.Event;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    /**
     * 사용자의 특정 기간동안의 모든 단일 일정 조회
     * @param memberId
     * @param startAt
     * @param endAt
     * @return
     */
    @Query("""
            SELECT e
            FROM Event e
            WHERE e.calendar.owner.memberId = :memberId
            AND e.calendar.team IS NULL
            AND e.recurrenceRule IS NULL
            AND e.endAt > :startAt
            AND e.startAt < :endAt
    """)
    List<Event> findPersonalScheduleInPeriod(
            @Param("memberId")
            Long memberId,
            @Param("startAt")
            LocalDateTime startAt,
            @Param("endAt")
            LocalDateTime endAt
    );

    /**
     * 여러 캘린더에서 특정 기간에 속하는 단일 이벤트 조회
     * @param calendarIds
     * @param startAt
     * @param endAt
     * @return
     */
    @Query("""
            SELECT e
            FROM Event e
            WHERE e.calendar.calendarId IN :calendarIds
            AND e.recurrenceRule IS NULL
            AND e.endAt > :startAt
            AND e.startAt < :endAt
    """)
    List<Event> findSingleEventsInPeriod(
            @Param("calendarIds")
            List<Long> calendarIds,
            @Param("startAt")
            LocalDateTime startAt,
            @Param("endAt")
            LocalDateTime endAt
    );

    /**
     * 여러 캘린더에서 특정 기간동안의 일정 존재 여부 확인
     * @param calendarIds
     * @param startAt
     * @param endAt
     * @return
     */
    @Query("""
            SELECT count(e) > 0
            FROM Event e
            WHERE e.calendar.calendarId = :calendarIds
            AND e.endAt > :startAt
            AND e.startAt < :endAt
    """)
    boolean existsSingleScheduleInPeriod(
            @Param("calendarIds")
            List<Long> calendarIds,
            @Param("startAt")
            LocalDateTime startAt,
            @Param("endAt")
            LocalDateTime endAt
    );

    /**
     * 여러 캘린더에서 특정 기간에 속하는 반복 이벤트 원본 조회
     * @param calendarIds
     * @param endAt
     * @return
     */
    @Query("""
            SELECT e
            FROM Event e
            WHERE e.calendar.calendarId IN :calendarIds
            AND e.recurrenceRule IS NOT NULL
            AND e.startAt < :endAt
    """)
    List<Event> findRecurringEventsInPeriod(
            @Param("calendarIds")
            List<Long> calendarIds,
            @Param("endAt")
            LocalDateTime endAt
    );

    @Query("""
            SELECT count(e) > 0
            FROM Event e
            WHERE e.calendar.calendarId IN :calendarIds
            AND e.eventId != :eventId
            AND e.recurrenceRule IS NULL
            AND e.endAt > :startAt
            AND e.startAt < :endAt
    """)
    boolean existsOtherSingleEventInPeriod(
            @Param("calendarIds")
            List<Long> calendarIds,
            @Param("startAt")
            LocalDateTime startAt,
            @Param("endAt")
            LocalDateTime endAt,
            @Param("eventId") Long excludeEventId
    );

    /**
     * 여러 캘린더의 특정 기간동안의 모든 일정 조회
     * @param calendarIds
     * @param startAt
     * @param endAt
     * @return
     */
    @Query("""
        SELECT e
        FROM Event e
        WHERE e.calendar.calendarId IN :calendarIds
        AND e.endAt > :startAt
        AND e.startAt < :endAt
        
    """)
    List<Event> findEventsInCalendarsInPeriod(
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
    boolean existsPersonalScheduleInPeriodExcludingEvent(
            @Param("calendarIds")
            List<Long> calendarIds,
            @Param("startAt")
            LocalDateTime startAt,
            @Param("endAt")
            LocalDateTime endAt,
            @Param("eventId")
            Long eventId
    );

    /**
     * 다가오는 일정 페이지 단위로 조회
     * @param memberId
     * @param now
     * @param pageable
     * @return
     */
    @Query("""
            SELECT e FROM Event e
            WHERE e.calendar.owner.memberId = :memberId
            AND e.startAt >= :now
            ORDER BY e.startAt ASC
    """)
    List<Event> findUpcomingEvents(
            @Param("memberId") Long memberId,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
