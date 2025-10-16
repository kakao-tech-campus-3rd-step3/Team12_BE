package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.domain.Event;
import unischedule.events.dto.EventServiceDto;
import unischedule.events.service.internal.EventRawService;
import unischedule.events.util.RRuleParser;
import unischedule.exception.InvalidInputException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
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
        eventList.addAll(eventRawService.findSingleSchedule(calendarIds, startAt, endAt));
        eventList.addAll(recurringEventService.expandRecurringEvents(calendarIds, startAt, endAt));

        return eventList;
    }

    /**
     * 새 단일 일정 시간 중복 체크
     * @param calendarIds
     * @param startAt
     * @param endAt
     */
    @Transactional(readOnly = true)
    public void checkNewSingleEventOverlap(
            List<Long> calendarIds,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        if (hasEvent(calendarIds, startAt, endAt)) {
            throw new InvalidInputException("겹치는 일정이 있어 등록할 수 없습니다.");
        }
    }

    /**
     * 새 반복 일정 시간 중복 체크
     * @param calendarIds
     * @param firstStartTime
     * @param firstEndTime
     * @param rruleString
     */
    @Transactional(readOnly = true)
    public void checkNewRecurringEventOverlap(
            List<Long> calendarIds,
            LocalDateTime firstStartTime,
            LocalDateTime firstEndTime,
            String rruleString
    ) {
        List<ZonedDateTime> eventStartTimes = rruleParser.calEventStartTimeListZdt(firstStartTime, rruleString);
        Duration duration = Duration.between(firstStartTime, firstEndTime);

        for (ZonedDateTime eventStartZdt : eventStartTimes) {
            LocalDateTime eventStartTime = eventStartZdt.toLocalDateTime();
            LocalDateTime eventEndTime = eventStartZdt.plus(duration).toLocalDateTime();

            checkNewSingleEventOverlap(calendarIds, eventStartTime, eventEndTime);
        }
    }

    @Transactional(readOnly = true)
    public void checkEventUpdateOverlap(
            List<Long> calendarIds,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Event excludeEvent
    ) {
        List<EventServiceDto> overlappingEvents = getEvents(calendarIds, startTime, endTime);

        boolean isOverlapping = overlappingEvents.stream()
                .anyMatch(event -> !Objects.equals(event.eventId(), excludeEvent.getEventId()));

        if (isOverlapping) {
            throw new InvalidInputException("해당 시간에 겹치는 일정이 있어 수정할 수 없습니다.");
        }
    }

    private boolean hasEvent(List<Long> calendarIds, LocalDateTime startAt, LocalDateTime endAt) {

        if (eventRawService.existsSingleSchedule(calendarIds, startAt, endAt)) {
            return true;
        }

        if (!recurringEventService.expandRecurringEvents(calendarIds, startAt, endAt).isEmpty()) {
            return true;
        }
        return false;
    }

}
