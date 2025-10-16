package unischedule.events.service.internal;

import lombok.RequiredArgsConstructor;
import net.fortuna.ical4j.model.Recur;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventException;
import unischedule.events.dto.EventUpdateDto;
import unischedule.events.repository.EventExceptionRepository;
import unischedule.events.repository.EventRepository;
import unischedule.events.util.RRuleParser;
import unischedule.events.util.ZonedDateTimeUtil;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.InvalidInputException;
import unischedule.member.domain.Member;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventRawService {
    private final EventRepository eventRepository;
    private final EventExceptionRepository eventExceptionRepository;
    private final RRuleParser rruleParser;
    private final ZonedDateTimeUtil zonedDateTimeUtil;

    @Transactional
    public Event saveEvent(Event event) {
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("해당 일정을 찾을 수 없습니다."));
    }

    @Transactional
    public void deleteEvent(Event event) {
        eventRepository.delete(event);
    }

    @Transactional
    public void updateEvent(Event event, EventUpdateDto updateDto) {
        event.modifyEvent(
                updateDto.title(),
                updateDto.content(),
                updateDto.startTime(),
                updateDto.endTime(),
                updateDto.isPrivate()
        );
    }

    @Transactional(readOnly = true)
    public void checkOverlapForNewSingleSchedule(List<Long> calendarIds, LocalDateTime startTime, LocalDateTime endTime) {
        if (eventRepository.existsSingleScheduleInPeriod(calendarIds, startTime, endTime)) {
            throw new InvalidInputException("겹치는 일정이 있어 등록할 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public void checkOverlapForNewRecurringSchedule(List<Long> calendarIds, LocalDateTime firstStartTime, LocalDateTime firstEndTime, String rruleString) {
        List<ZonedDateTime> eventStartTimeListZdt = rruleParser.calEventStartTimeListZdt(firstStartTime, rruleString);
        Duration duration = Duration.between(firstStartTime, firstEndTime);

        for (ZonedDateTime startZdt : eventStartTimeListZdt) {
            LocalDateTime eventStartTime = startZdt.toLocalDateTime();
            LocalDateTime eventEndTime = startZdt.plus(duration).toLocalDateTime();
            checkOverlapForNewSingleSchedule(calendarIds, eventStartTime, eventEndTime);
        }
    }

    @Transactional(readOnly = true)
    public void validateNoScheduleForMembers(List<Member> memberList, LocalDateTime startTime, LocalDateTime endTime) {
        if (memberList.isEmpty()) return;
        List<Long> memberIds = memberList
                .stream()
                .map(Member::getMemberId)
                .toList();

        if (eventRepository.existsScheduleForMembers(memberIds, startTime, endTime)) {
            throw new InvalidInputException("일정이 겹치는 멤버가 있습니다.");
        }
    }

    @Transactional(readOnly = true)
    public void canUpdateEventForMembers(List<Member> memberList, Event event, LocalDateTime startTime, LocalDateTime endTime) {
        if (memberList.isEmpty()) return;
        List<Long> memberIds = memberList
                .stream()
                .map(Member::getMemberId)
                .toList();

        if (eventRepository.existsScheduleForMembersExcludingEvent(
                memberIds,
                startTime,
                endTime,
                event.getEventId())
        ) {
            throw new InvalidInputException("일정이 겹치는 멤버가 있어서 일정을 수정할 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<Event> findSchedule(List<Long> calendarIds, LocalDateTime startTime, LocalDateTime endTime) {
        return eventRepository.findEventsInCalendarsInPeriod(calendarIds, startTime, endTime);
    }

    @Transactional(readOnly = true)
    public List<Event> findSingleSchedule(List<Long> calendarIds, LocalDateTime startTime, LocalDateTime endTime) {
        return eventRepository.findSingleEventsInPeriod(calendarIds, startTime, endTime);
    }

    @Transactional(readOnly = true)
    public List<Event> findRecurringSchedule(List<Long> calendarIds, LocalDateTime endTime) {
        return eventRepository.findRecurringEventsInPeriod(calendarIds, endTime);
    }

    @Transactional(readOnly = true)
    public void canUpdateEvent(List<Long> calendarIds, Event event, LocalDateTime startTime, LocalDateTime endTime) {
        if (eventRepository.existsPersonalScheduleInPeriodExcludingEvent(calendarIds, startTime, endTime, event.getEventId())) {
            throw new InvalidInputException("해당 시간에 겹치는 일정이 있어 수정할 수 없습니다.");
        }
    }
    
    @Transactional(readOnly = true)
    public List<Event> findUpcomingEventsByMember(Member member) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 3); // 개수 제한
        return eventRepository.findUpcomingEvents(member.getMemberId(), now, pageable);
    }
    
    @Transactional(readOnly = true)
    public List<Event> findTodayEventsByMember(Member member) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();              // 오늘 00:00
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();    // 내일 00:00
        return eventRepository.findPersonalScheduleInPeriod(member.getMemberId(), startOfDay, endOfDay);
    }

    @Transactional(readOnly = true)
    public List<Event> expandRecurringEvents(List<Long> calendarIds, LocalDateTime startAt, LocalDateTime endAt) {
        List<Event> recurringEvents = findRecurringSchedule(calendarIds, endAt);

        List<Event> expandedEventList = new ArrayList<>();

        Map<Long, List<EventException>> exceptionsMap = getEventExceptionMap(recurringEvents, startAt, endAt);

        for (Event recurEvent : recurringEvents) {
            List<Event> expandedEvent = expandRecurringEvent(recurEvent, startAt, endAt);
            List<EventException> exceptions = exceptionsMap.getOrDefault(recurEvent.getEventId(), List.of());
            expandedEventList.addAll(applyEventExceptions(expandedEvent, exceptions));
        }

        return expandedEventList;
    }

    private Map<Long, List<EventException>> getEventExceptionMap(List<Event> recurringEvents, LocalDateTime startAt, LocalDateTime endAt) {
        return eventExceptionRepository
                .findEventExceptionsForEvents(recurringEvents, startAt, endAt)
                .stream()
                .collect(Collectors.groupingBy(ex -> ex.getOriginalEvent().getEventId()));
    }

    private List<Event> expandRecurringEvent(Event recEvent, LocalDateTime startAt, LocalDateTime endAt) {
        Recur<ZonedDateTime> recur = rruleParser.getRecur(recEvent.getRecurrenceRule().getRruleString());

        ZonedDateTime seed = zonedDateTimeUtil.localDateTimeToZdt(recEvent.getStartAt());
        ZonedDateTime startZdt = zonedDateTimeUtil.localDateTimeToZdt(startAt);
        ZonedDateTime endZdt = zonedDateTimeUtil.localDateTimeToZdt(endAt);

        Duration duration = Duration.between(seed.toLocalDateTime(), recEvent.getEndAt());

        List<ZonedDateTime> dates = recur.getDates(startZdt, endZdt);

        return dates.stream()
                .filter(eventStart -> !eventStart.isBefore(startZdt) && eventStart.isBefore(endZdt))
                .map(eventStart -> Event.builder()
                        .title(recEvent.getTitle())
                        .content(recEvent.getContent())
                        .startAt(eventStart.toLocalDateTime())
                        .endAt(eventStart.toLocalDateTime().plus(duration))
                        .isPrivate(recEvent.getIsPrivate())
                        .state(recEvent.getState())
                        .build())
                .toList();
    }

    private List<Event> applyEventExceptions(List<Event> expandedEvents, List<EventException> exceptions) {
        if (exceptions.isEmpty()) {
            return expandedEvents;
        }

        Map<LocalDateTime, EventException> exceptionMap = exceptions.stream()
                .collect(Collectors.toMap(EventException::getOriginalEventTime, ex -> ex));

        List<Event> finalEvents = new ArrayList<>();

        for (Event event : expandedEvents) {
            if (exceptionMap.containsKey(event.getStartAt())) {
                EventException exception = exceptionMap.get(event.getStartAt());

                if (isDeletionException(exception)) {
                    continue;
                }

                finalEvents.add(applyException(event, exception));
            }
            else {
                finalEvents.add(event);
            }
        }
        return finalEvents;
    }

    private boolean isDeletionException(EventException eventException) {
        return eventException.getTitle() == null;
    }

    private Event applyException(Event event, EventException eventException) {
        return Event.builder()
                .title(eventException.getTitle())
                .content(eventException.getContent())
                .startAt(eventException.getStartAt())
                .endAt(eventException.getEndAt())
                .isPrivate(eventException.getIsPrivate())
                .build();
    }
}
