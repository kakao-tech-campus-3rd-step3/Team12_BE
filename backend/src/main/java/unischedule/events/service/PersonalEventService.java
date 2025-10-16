package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import net.fortuna.ical4j.model.Recur;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventException;
import unischedule.events.domain.EventState;
import unischedule.events.domain.RecurrenceRule;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.ExpandedRecurringEventDeleteRequestDto;
import unischedule.events.dto.ExpandedRecurringEventModifyRequestDto;
import unischedule.events.dto.PersonalEventCreateRequestDto;
import unischedule.events.dto.RecurringEventCreateRequestDto;
import unischedule.events.repository.EventExceptionRepository;
import unischedule.events.service.internal.EventRawService;
import unischedule.events.util.RRuleParser;
import unischedule.events.util.ZonedDateTimeUtil;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.service.internal.TeamMemberRawService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalEventService {
    private final MemberRawService memberRawService;
    private final EventRawService eventRawService;
    private final TeamMemberRawService teamMemberRawService;
    private final CalendarRawService calendarRawService;
    private final EventExceptionRepository eventExceptionRepository;
    private final RRuleParser rruleParser;
    private final ZonedDateTimeUtil zonedDateTimeUtil;

    @Transactional
    public EventCreateResponseDto makePersonalSingleEvent(String email, PersonalEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);

        Calendar targetCalendar = calendarRawService.getMyPersonalCalendar(member);

        targetCalendar.validateOwner(member);

        eventRawService.checkOverlapForNewSingleSchedule(List.of(targetCalendar.getCalendarId()), requestDto.startTime(), requestDto.endTime());

        Event newEvent = Event.builder()
                .title(requestDto.title())
                .content(requestDto.description())
                .startAt(requestDto.startTime())
                .endAt(requestDto.endTime())
                .state(EventState.CONFIRMED)
                .isPrivate(requestDto.isPrivate())
                .build();

        newEvent.connectCalendar(targetCalendar);
        Event saved = eventRawService.saveEvent(newEvent);

        return EventCreateResponseDto.from(saved);
    }

    @Transactional
    public EventCreateResponseDto makePersonalRecurringEvent(String email, RecurringEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);

        Calendar targetCalendar = calendarRawService.getMyPersonalCalendar(member);

        targetCalendar.validateOwner(member);

        eventRawService.checkOverlapForNewRecurringSchedule(
                List.of(targetCalendar.getCalendarId()),
                requestDto.firstStartTime(),
                requestDto.firstEndTime(),
                requestDto.rrule()
        );

        Event newEvent = Event.builder()
                .title(requestDto.title())
                .content(requestDto.description())
                .startAt(requestDto.firstStartTime())
                .endAt(requestDto.firstEndTime())
                .state(EventState.CONFIRMED)
                .isPrivate(requestDto.isPrivate())
                .build();

        RecurrenceRule rule = new RecurrenceRule(requestDto.rrule());
        newEvent.connectRecurrenceRule(rule);
        newEvent.connectCalendar(targetCalendar);

        Event saved = eventRawService.saveEvent(newEvent);

        return EventCreateResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getPersonalEvents(String email, LocalDateTime startAt, LocalDateTime endAt) {
        Member member = memberRawService.findMemberByEmail(email);

        List<Team> teamList = teamMemberRawService.findByMember(member)
                .stream()
                .map(TeamMember::getTeam)
                .toList();

        List<Long> calendarIds = getMemberCalendarIds(teamList, member);

        List<EventGetResponseDto> findEvents = new ArrayList<>();
        addSingleSchedule(calendarIds, findEvents, startAt, endAt);
        addExpandedEventsFromRecurringEvents(calendarIds, findEvents, startAt, endAt);

        return findEvents;
    }

    private void addSingleSchedule(List<Long> calendarIds, List<EventGetResponseDto> findEvents, LocalDateTime startAt, LocalDateTime endAt) {
        eventRawService.findSingleSchedule(calendarIds, startAt, endAt)
                .stream()
                .map(EventGetResponseDto::fromSingleEvent)
                .forEach(findEvents::add);
    }

    private void addExpandedEventsFromRecurringEvents(List<Long> calendarIds, List<EventGetResponseDto> findEvents, LocalDateTime startAt, LocalDateTime endAt) {
        expandRecurringEvents(calendarIds, startAt, endAt)
                .stream()
                .map(EventGetResponseDto::fromRecurringEvent)
                .forEach(findEvents::add);
    }

    private List<Event> expandRecurringEvents(List<Long> calendarIds, LocalDateTime startAt, LocalDateTime endAt) {
        List<Event> recurringEvents = eventRawService.findRecurringSchedule(calendarIds, endAt);

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

    @Transactional
    public EventGetResponseDto modifyPersonalEvent(String email, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);

        Event findEvent = eventRawService.findEventById(requestDto.eventId());

        findEvent.validateEventOwner(member);

        eventRawService.canUpdateEvent(List.of(findEvent.getCalendar().getCalendarId()), findEvent, requestDto.startTime(), requestDto.endTime());

        eventRawService.updateEvent(findEvent, EventModifyRequestDto.toDto(requestDto));
        
        return EventGetResponseDto.fromSingleEvent(findEvent);
    }

    @Transactional
    public EventGetResponseDto modifyPersonalExpandedRecurringEvent(String email, Long eventId, ExpandedRecurringEventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);

        Event originalEvent = eventRawService.findEventById(eventId);

        originalEvent.validateEventOwner(member);

        EventException eventException = EventException.makeEventException(originalEvent, requestDto.toEventExceptionDto());

        EventException savedException = eventExceptionRepository.save(eventException);

        return EventGetResponseDto.fromRecurringEvent(savedException.toEvent());
    }

    @Transactional
    public void deletePersonalExpandedRecurringEvent(String email, Long eventId, ExpandedRecurringEventDeleteRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event originalEvent = eventRawService.findEventById(eventId);

        originalEvent.validateEventOwner(member);

        EventException eventException = EventException.makeEventDeleteException(originalEvent, requestDto.originalStartTime());
        eventExceptionRepository.save(eventException);
    }

    @Transactional
    public void deletePersonalEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);

        Event event = eventRawService.findEventById(eventId);

        event.validateEventOwner(member);

        eventRawService.deleteEvent(event);
    }

    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getUpcomingMyEvent(String email) {
        Member member = memberRawService.findMemberByEmail(email);
        
        List<Event> upcomingEvents = eventRawService.findUpcomingEventsByMember(member);
        
        return upcomingEvents.stream().map(EventGetResponseDto::fromSingleEvent).toList();
    }

    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getTodayMyEvent(String email) {
        Member member = memberRawService.findMemberByEmail(email);
        
        List<Event> todayEvents = eventRawService.findTodayEventsByMember(member);
        
        return todayEvents.stream().map(EventGetResponseDto::fromSingleEvent).toList();
    }

    private List<Long> getMemberCalendarIds(List<Team> teamList, Member member) {
        List<Long> calendarIds = new ArrayList<>();

        // 팀 캘린더
        List<Long> teamCalendarIds = teamList.stream()
                .map(calendarRawService::getTeamCalendar)
                .map(Calendar::getCalendarId)
                .toList();

        // 개인 캘린더
        calendarIds.add(calendarRawService.getMyPersonalCalendar(member).getCalendarId());

        calendarIds.addAll(teamCalendarIds);
        return calendarIds;
    }
}
