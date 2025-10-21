package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;
import unischedule.events.domain.EventState;
import unischedule.events.domain.RecurrenceRule;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.PersonalEventCreateRequestDto;
import unischedule.events.dto.RecurringEventCreateRequestDto;
import unischedule.events.dto.RecurringInstanceDeleteRequestDto;
import unischedule.events.dto.RecurringInstanceModifyRequestDto;
import unischedule.events.service.common.EventQueryService;
import unischedule.events.service.internal.EventOverrideRawService;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PersonalEventService {
    private final MemberRawService memberRawService;
    private final EventRawService eventRawService;
    private final RecurringEventRawService recurringEventRawService;
    private final EventQueryService eventQueryService;
    private final TeamMemberRawService teamMemberRawService;
    private final CalendarRawService calendarRawService;
    private final EventOverrideRawService eventOverrideRawService;

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
    public EventGetResponseDto modifyPersonalEvent(String email, Long eventId, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);

        Event findEvent = eventRawService.findEventById(eventId);

        findEvent.validateEventOwner(member);

        modifyEvent(requestDto, findEvent);

        return EventGetResponseDto.fromSingleEvent(findEvent);
    }

    @Transactional
    public EventGetResponseDto modifyRecurringEvent(String email, Long eventId, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event foundEvent = eventRawService.findEventById(eventId);
        foundEvent.validateEventOwner(member);

        modifyEvent(requestDto, foundEvent);
        eventOverrideRawService.deleteAllEventOverrideByEvent(foundEvent);

        return EventGetResponseDto.fromRecurringEvent(foundEvent);
    }

    private void modifyEvent(EventModifyRequestDto requestDto, Event originalEvent) {

        eventQueryService.checkEventUpdateOverlap(
                List.of(originalEvent.getCalendar().getCalendarId()),
                getValueOrDefault(requestDto.startTime(), originalEvent.getStartAt()),
                getValueOrDefault(requestDto.endTime(), originalEvent.getEndAt()),
                originalEvent
        );

        eventRawService.updateEvent(originalEvent, EventModifyRequestDto.toDto(requestDto));
    }

    private static <T> T getValueOrDefault(T value, T defaultValue) {
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    @Transactional
    public EventGetResponseDto modifyPersonalRecurringInstance(String email, Long eventId, RecurringInstanceModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event originalEvent = eventRawService.findEventById(eventId);
        originalEvent.validateEventOwner(member);

        Optional<EventOverride> eventOverrideOpt = eventOverrideRawService.findEventOverride(originalEvent, requestDto.originalStartTime());

        EventOverride eventOverride;
        if (eventOverrideOpt.isPresent()) {
            eventOverride = eventOverrideOpt.get();
            eventOverrideRawService.updateEventOverride(eventOverride, requestDto.toEventOverrideDto());
        }
        else {
            eventOverride = EventOverride.makeEventOverride(originalEvent, requestDto.toEventOverrideDto());
        }

        EventOverride savedOverride = eventOverrideRawService.saveEventOverride(eventOverride);

        return EventGetResponseDto.fromEventOverride(savedOverride, originalEvent);
    }

    @Transactional
    public void deletePersonalRecurringInstance(String email, Long eventId, RecurringInstanceDeleteRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event originalEvent = eventRawService.findEventById(eventId);

        originalEvent.validateEventOwner(member);

        EventOverride eventOverride = EventOverride.makeEventDeleteOverride(originalEvent, requestDto.originalStartTime());
        eventOverrideRawService.saveEventOverride(eventOverride);
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
