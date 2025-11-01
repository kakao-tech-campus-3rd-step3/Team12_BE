package unischedule.events.service.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.domain.Event;
import unischedule.events.domain.collection.SingleEventSeries;
import unischedule.events.dto.EventServiceDto;
import unischedule.events.service.internal.EventRawService;
import unischedule.events.util.RRuleParser;
import unischedule.exception.InvalidInputException;
import unischedule.member.domain.Member;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EventQueryService {
    private final EventRawService eventRawService;
    private final RecurringEventService recurringEventService;
    private final RRuleParser rruleParser;

    @Transactional(readOnly = true)
    public List<EventServiceDto> getEvents(List<Long> calendarIds, LocalDateTime startAt, LocalDateTime endAt) {
        List<EventServiceDto> eventList = new ArrayList<>();

        SingleEventSeries singleEvents = eventRawService.findSingleSchedule(calendarIds, startAt, endAt);

        eventList.addAll(singleEvents.toServiceDtos());
        eventList.addAll(recurringEventService.expandRecurringEvents(calendarIds, startAt, endAt));

        return eventList;
    }

    /**
     * 특정 멤버의 일정 조회
     * @param member
     * @param calendarIds
     * @param startAt
     * @param endAt
     * @return
     */
    @Transactional(readOnly = true)
    public List<EventServiceDto> getEventsForMember(Member member, List<Long> calendarIds, LocalDateTime startAt, LocalDateTime endAt) {
        List<EventServiceDto> eventList = new ArrayList<>();

        SingleEventSeries singleEvents = eventRawService.findSingleScheduleForMember(member, calendarIds, startAt, endAt);
        eventList.addAll(singleEvents.toServiceDtos());
        eventList.addAll(recurringEventService.expandRecurringEventsForMember(member, calendarIds, startAt, endAt));

        return eventList;
    }

    @Transactional(readOnly = true)
    public void checkNewSingleEventOverlapForMember(
            Member member,
            List<Long> calendarIds,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        if (hasEventForMember(member, calendarIds, startAt, endAt)) {
            throw new InvalidInputException("겹치는 일정이 있어 등록할 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public void checkNewRecurringEventOverlapForMember(
            Member member,
            List<Long> calendarIds,
            LocalDateTime firstStartTime,
            LocalDateTime firstEndTime,
            String rruleString
    ) {
        List<LocalDateTime> eventStartTimes = rruleParser.calEventStartTimeList(firstStartTime, rruleString);

        Duration duration = Duration.between(firstStartTime, firstEndTime);

        for (LocalDateTime eventStartTime : eventStartTimes) {
            LocalDateTime eventEndTime = eventStartTime.plus(duration);

            checkNewSingleEventOverlapForMember(member, calendarIds, eventStartTime, eventEndTime);
        }
    }

    /**
     * 특정 멤버 기준 일정 수정 시 시간 중복 체크
     * @param member
     * @param calendarIds
     * @param startTime
     * @param endTime
     * @param excludeEvent
     */
    @Transactional(readOnly = true)
    public void checkEventUpdateOverlapForMember(
            Member member,
            List<Long> calendarIds,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Event excludeEvent
    ) {
        List<EventServiceDto> overlappingEvents = getEventsForMember(member, calendarIds, startTime, endTime);

        boolean isOverlapping = overlappingEvents.stream()
                .anyMatch(event -> !Objects.equals(event.eventId(), excludeEvent.getEventId()));

        if (isOverlapping) {
            throw new InvalidInputException("해당 시간에 겹치는 일정이 있어 수정할 수 없습니다.");
        }
    }

    /**
     * 해당 시간에 참여하는 일정이 있는지 확인
     * @param member
     * @param calendarIds
     * @param startAt
     * @param endAt
     * @return
     */
    private boolean hasEventForMember(Member member, List<Long> calendarIds, LocalDateTime startAt, LocalDateTime endAt) {
        SingleEventSeries singleEvents = eventRawService.findSingleScheduleForMember(member, calendarIds, startAt, endAt);
        if (singleEvents.hasOverlap(startAt, endAt)) {
            return true;
        }

        return !recurringEventService.expandRecurringEventsForMember(member, calendarIds, startAt, endAt).isEmpty();
    }

}
