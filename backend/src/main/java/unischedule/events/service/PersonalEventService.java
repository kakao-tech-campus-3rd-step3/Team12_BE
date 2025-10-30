package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.EventServiceDto;
import unischedule.events.dto.PersonalEventCreateRequestDto;
import unischedule.events.dto.RecurringEventCreateRequestDto;
import unischedule.events.dto.RecurringInstanceDeleteRequestDto;
import unischedule.events.dto.RecurringInstanceModifyRequestDto;
import unischedule.events.service.common.EventCommandService;
import unischedule.events.service.common.EventQueryService;
import unischedule.events.service.internal.EventRawService;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.service.internal.TeamMemberRawService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalEventService {
    private final MemberRawService memberRawService;
    private final EventRawService eventRawService;
    private final EventQueryService eventQueryService;
    private final TeamMemberRawService teamMemberRawService;
    private final CalendarRawService calendarRawService;
    private final EventCommandService eventCommandService;

    @Transactional
    public EventCreateResponseDto makePersonalSingleEvent(String email, PersonalEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Calendar targetCalendar = calendarRawService.getMyPersonalCalendar(member);
        targetCalendar.validateOwner(member);

        List<Long> conflictCheckCalendarIds = getMemberCalendarIds(member);
        Event saved = eventCommandService.createSingleEvent(targetCalendar, conflictCheckCalendarIds, requestDto.toDto());

        return EventCreateResponseDto.from(saved);
    }

    @Transactional
    public EventCreateResponseDto makePersonalRecurringEvent(String email, RecurringEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Calendar targetCalendar = calendarRawService.getMyPersonalCalendar(member);
        targetCalendar.validateOwner(member);

        List<Long> conflictCheckCalendarIds = getMemberCalendarIds(member);
        Event saved = eventCommandService.createRecurringEvent(targetCalendar, conflictCheckCalendarIds, requestDto);

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

        List<Long> calendarIds = getMemberCalendarIds(member);

        return eventQueryService.getEventsForMember(member, calendarIds, startAt, endAt)
                .stream()
                .map(EventGetResponseDto::fromServiceDto)
                .toList();
    }

    @Transactional
    public EventGetResponseDto modifyPersonalEvent(String email, Long eventId, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event findEvent = eventRawService.findEventById(eventId);
        findEvent.validateEventOwner(member);

        List<Long> conflictCheckCalendarIds = getMemberCalendarIds(member);
        eventCommandService.modifySingleEvent(findEvent, conflictCheckCalendarIds, requestDto.toDto());

        return EventGetResponseDto.fromSingleEvent(findEvent);
    }

    @Transactional
    public EventGetResponseDto modifyRecurringEvent(String email, Long eventId, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event foundEvent = eventRawService.findEventById(eventId);
        foundEvent.validateEventOwner(member);

        List<Long> conflictCheckCalendarIds = getMemberCalendarIds(member);
        eventCommandService.modifyRecurringEvent(foundEvent, conflictCheckCalendarIds, requestDto.toDto());

        return EventGetResponseDto.fromRecurringEvent(foundEvent);
    }

    @Transactional
    public EventGetResponseDto modifyPersonalRecurringInstance(String email, Long eventId, RecurringInstanceModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event originalEvent = eventRawService.findEventById(eventId);
        originalEvent.validateEventOwner(member);

        EventOverride savedOverride = eventCommandService.modifyRecurringInstance(originalEvent, requestDto);

        return EventGetResponseDto.fromEventOverride(savedOverride, originalEvent);
    }

    @Transactional
    public void deletePersonalRecurringInstance(String email, Long eventId, RecurringInstanceDeleteRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event originalEvent = eventRawService.findEventById(eventId);

        originalEvent.validateEventOwner(member);

        eventCommandService.deleteRecurringEventInstance(originalEvent, requestDto);
    }

    @Transactional
    public void deletePersonalEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);

        Event event = eventRawService.findEventById(eventId);

        event.validateEventOwner(member);

        eventCommandService.deleteSingleEvent(event);
    }

    @Transactional
    public void deleteRecurringEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);

        Event event = eventRawService.findEventById(eventId);

        event.validateEventOwner(member);

        eventCommandService.deleteRecurringEvent(event);
    }
    
    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getTodayMyEvent(String email) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        return getEventsForPeriod(email, start, end);
    }
    
    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getUpcomingMyEvent(String email) {
        LocalDateTime start = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(8).atStartOfDay();
        return getEventsForPeriod(email, start, end);
    }
    
    private List<EventGetResponseDto> getEventsForPeriod(String email, LocalDateTime start, LocalDateTime end) {
        Member member = memberRawService.findMemberByEmail(email);
        
        List<Long> calendarIds = getMemberCalendarIds(member);
        
        List<EventServiceDto> events = eventQueryService.getEventsForMember(member, calendarIds, start, end);
        
        return events.stream()
            .map(EventGetResponseDto::fromServiceDto)
            .toList();
    }

    private List<Long> getMemberCalendarIds(Member member) {
        List<Team> teamList = teamMemberRawService.findByMember(member)
                .stream()
                .map(TeamMember::getTeam)
                .toList();

        List<Long> calendarIds = new ArrayList<>();

        // 개인 캘린더
        calendarIds.add(calendarRawService.getMyPersonalCalendar(member).getCalendarId());

        // 팀 캘린더
        List<Long> teamCalendarIds = teamList.stream()
                .map(calendarRawService::getTeamCalendar)
                .map(Calendar::getCalendarId)
                .toList();

        calendarIds.addAll(teamCalendarIds);
        return calendarIds;
    }
}
