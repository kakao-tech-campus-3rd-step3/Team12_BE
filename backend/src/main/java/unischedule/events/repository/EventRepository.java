package unischedule.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import unischedule.calendar.entity.Calendar;
import unischedule.events.domain.Event;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
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
     * 기간 내 단일 이벤트 조회
     * - 개인 일정 (team == null)
     * - 팀 전체 일정 (isSelective = false || null)
     * - 팀 선택 일정 (isSelective = true && 참여)
     * @param memberId
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
            AND (e.calendar.team IS NULL OR e.isSelective IS NULL OR e.isSelective = false OR
                    (e.isSelective = true AND EXISTS (
                        SELECT 1 FROM EventParticipant ep
                        WHERE ep.event = e AND ep.member.memberId = :memberId
                ))
            )
    """)
    List<Event> findSingleEventsInPeriodForMember(
            @Param("memberId")
            Long memberId,
            @Param("calendarIds")
            List<Long> calendarIds,
            @Param("startAt")
            LocalDateTime startAt,
            @Param("endAt")
            LocalDateTime endAt
    );

    @Query("""
            SELECT e
            FROM Event e
            WHERE e.calendar.calendarId IN :calendarIds
            AND e.recurrenceRule IS NOT NULL
            AND e.startAt < :endAt
            AND (e.calendar.team IS NULL OR e.isSelective IS NULL OR e.isSelective = false OR
                    (e.isSelective = true AND EXISTS (
                        SELECT 1 FROM EventParticipant ep
                        WHERE ep.event = e AND ep.member.memberId = :memberId
                ))
            )
    """)
    List<Event> findRecurringEventsInPeriodForMember(
            @Param("memberId") Long memberId,
            @Param("calendarIds") List<Long> calendarIds,
            @Param("endAt") LocalDateTime endAt
    );

    /**
     * 여러 캘린더에서 특정 기간동안의 단일 일정 존재 여부 확인
     * @param calendarIds
     * @param startAt
     * @param endAt
     * @return
     */
    @Query("""
            SELECT count(e) > 0
            FROM Event e
            WHERE e.calendar.calendarId IN :calendarIds
            AND e.recurrenceRule IS NULL
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

    List<Event> findByCalendar(Calendar calendar);

    void deleteAll(Iterable<? extends Event> entities);
}
