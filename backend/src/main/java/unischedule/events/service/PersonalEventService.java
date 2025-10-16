package unischedule.events.service;

import lombok.RequiredArgsConstructor;
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
import unischedule.events.dto.PersonalEventCreateRequestDto;
import unischedule.events.dto.RecurringEventCreateRequestDto;
import unischedule.events.dto.RecurringInstanceDeleteRequestDto;
import unischedule.events.dto.RecurringInstanceModifyRequestDto;
import unischedule.events.service.internal.EventExceptionRawService;
import unischedule.events.service.internal.EventRawService;
import unischedule.events.service.internal.RecurringEventRawService;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.service.internal.TeamMemberRawService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalEventService {
    private final MemberRawService memberRawService;
    private final EventRawService eventRawService;
    private final RecurringEventRawService recurringEventRawService;
    private final EventQueryService eventQueryService;
    private final TeamMemberRawService teamMemberRawService;
    private final CalendarRawService calendarRawService;
    private final EventExceptionRawService eventExceptionRawService;

    @Transactional
    public EventCreateResponseDto makePersonalSingleEvent(String email, PersonalEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);

        Calendar targetCalendar = calendarRawService.getMyPersonalCalendar(member);

        targetCalendar.validateOwner(member);

        eventQueryService.checkNewSingleEventOverlap(List.of(targetCalendar.getCalendarId()), requestDto.startTime(), requestDto.endTime());

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

        eventQueryService.checkNewRecurringEventOverlap(
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
    public EventGetResponseDto getPersonalEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);
        Event event = eventRawService.findEventById(eventId);

        event.validateEventOwner(member);

        if (event.getRecurrenceRule() == null) {
            return EventGetResponseDto.fromSingleEvent(event);
        }
        else {
            return EventGetResponseDto.fromRecurringEvent(event);
        }
    }

    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getPersonalEvents(String email, LocalDateTime startAt, LocalDateTime endAt) {
        Member member = memberRawService.findMemberByEmail(email);

        List<Team> teamList = teamMemberRawService.findByMember(member)
                .stream()
                .map(TeamMember::getTeam)
                .toList();

        List<Long> calendarIds = getMemberCalendarIds(teamList, member);

        return eventQueryService.getEvents(calendarIds, startAt, endAt)
                .stream()
                .map(EventGetResponseDto::fromServiceDto)
                .toList();
    }

    @Transactional
    public EventGetResponseDto modifyPersonalEvent(String email, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);

        Event findEvent = eventRawService.findEventById(requestDto.eventId());

        findEvent.validateEventOwner(member);

        modifyEvent(requestDto, findEvent);

        return EventGetResponseDto.fromSingleEvent(findEvent);
    }

    @Transactional
    public EventGetResponseDto modifyRecurringEvent(String email, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event foundEvent = eventRawService.findEventById(requestDto.eventId());
        foundEvent.validateEventOwner(member);

        modifyEvent(requestDto, foundEvent);
        eventExceptionRawService.deleteAllEventExceptionByEvent(foundEvent);

        return EventGetResponseDto.fromRecurringEvent(foundEvent);
    }

    private void modifyEvent(EventModifyRequestDto requestDto, Event originalEvent) {
        eventQueryService.checkEventUpdateOverlap(
                List.of(originalEvent.getCalendar().getCalendarId()),
                requestDto.startTime(),
                requestDto.endTime(),
                originalEvent
        );

        eventRawService.updateEvent(originalEvent, EventModifyRequestDto.toDto(requestDto));
    }

    @Transactional
    public EventGetResponseDto modifyPersonalExpandedRecurringEvent(String email, Long eventId, RecurringInstanceModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);

        Event originalEvent = eventRawService.findEventById(eventId);

        originalEvent.validateEventOwner(member);

        EventException eventException = EventException.makeEventException(originalEvent, requestDto.toEventExceptionDto());

        EventException savedException = eventExceptionRawService.saveEventException(eventException);

        return EventGetResponseDto.fromEventException(savedException, originalEvent);
    }

    @Transactional
    public void deletePersonalExpandedRecurringEvent(String email, Long eventId, RecurringInstanceDeleteRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event originalEvent = eventRawService.findEventById(eventId);

        originalEvent.validateEventOwner(member);

        EventException eventException = EventException.makeEventDeleteException(originalEvent, requestDto.originalStartTime());
        eventExceptionRawService.saveEventException(eventException);
    }

    @Transactional
    public void deletePersonalEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);

        Event event = eventRawService.findEventById(eventId);

        event.validateEventOwner(member);

        eventRawService.deleteEvent(event);
    }

    @Transactional
    public void deleteRecurringEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);

        Event event = eventRawService.findEventById(eventId);

        event.validateEventOwner(member);

        recurringEventRawService.deleteRecurringEvent(event);
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
