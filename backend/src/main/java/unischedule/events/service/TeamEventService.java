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
import unischedule.events.dto.EventServiceDto;
import unischedule.events.dto.RecurringEventCreateRequestDto;
import unischedule.events.dto.RecurringInstanceDeleteRequestDto;
import unischedule.events.dto.RecurringInstanceModifyRequestDto;
import unischedule.events.dto.TeamEventCreateRequestDto;
import unischedule.events.service.common.EventQueryService;
import unischedule.events.service.internal.EventOverrideRawService;
import unischedule.events.service.internal.EventRawService;
import unischedule.events.service.internal.RecurringEventRawService;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.team.service.internal.TeamRawService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeamEventService {
    private final MemberRawService memberRawService;
    private final EventRawService eventRawService;
    private final RecurringEventRawService recurringEventRawService;
    private final CalendarRawService calendarRawService;
    private final TeamRawService teamRawService;
    private final TeamMemberRawService teamMemberRawService;
    private final EventQueryService eventQueryService;
    private final EventOverrideRawService eventOverrideRawService;

    @Transactional
    public EventCreateResponseDto createTeamSingleEvent(String email, TeamEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Team team = teamRawService.findTeamById(requestDto.teamId());
        validateTeamMember(team, member);

        List<Long> calendarIdsToValidate = getAllRelatedCalendarIds(team);
        eventQueryService.checkNewSingleEventOverlap(calendarIdsToValidate, requestDto.startTime(), requestDto.endTime());

        Calendar calendar = calendarRawService.getTeamCalendar(team);
        Event event = new Event(
                requestDto.title(),
                requestDto.description(),
                requestDto.startTime(),
                requestDto.endTime(),
                EventState.CONFIRMED,
                requestDto.isPrivate()
        );
        event.connectCalendar(calendar);

        return EventCreateResponseDto.from(eventRawService.saveEvent(event));
    }

    @Transactional
    public EventCreateResponseDto createTeamRecurringEvent(String email, Long teamId, RecurringEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Team team = teamRawService.findTeamById(teamId);
        validateTeamMember(team, member);

        List<Long> calendarIdsToValidate = getAllRelatedCalendarIds(team);
        eventQueryService.checkNewRecurringEventOverlap(
                calendarIdsToValidate,
                requestDto.firstStartTime(),
                requestDto.firstEndTime(),
                requestDto.rrule()
        );

        Calendar calendar = calendarRawService.getTeamCalendar(team);
        Event event = new Event(
                requestDto.title(),
                requestDto.description(),
                requestDto.firstStartTime(),
                requestDto.firstEndTime(),
                EventState.CONFIRMED,
                requestDto.isPrivate()
        );

        RecurrenceRule recurrenceRule = new RecurrenceRule(requestDto.rrule());
        event.connectCalendar(calendar);
        event.connectRecurrenceRule(recurrenceRule);

        return EventCreateResponseDto.from(eventRawService.saveEvent(event));
    }

    @Transactional(readOnly = true)
    public EventGetResponseDto getTeamEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);
        Event event = eventRawService.findEventById(eventId);

        event.validateIsTeamEvent();

        Team team = event.getCalendar().getTeam();
        validateTeamMember(team, member);

        if (event.getRecurrenceRule() == null) {
            return EventGetResponseDto.fromSingleEvent(event);
        }
        else {
            return EventGetResponseDto.fromRecurringEvent(event);
        }
    }

    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getTeamEvents(String email, Long teamId, LocalDateTime startAt, LocalDateTime endAt) {
        Member member = memberRawService.findMemberByEmail(email);
        Team team = teamRawService.findTeamById(teamId);

        validateTeamMember(team, member);

        Calendar teamCalendar = calendarRawService.getTeamCalendar(team);

        return eventQueryService.getEvents(List.of(teamCalendar.getCalendarId()), startAt, endAt)
                .stream()
                .map(EventGetResponseDto::fromServiceDto)
                .toList();
    }

    @Transactional
    public EventGetResponseDto modifyTeamEvent(String email, Long eventId, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);

        Event event = eventRawService.findEventById(eventId);

        Team team = event.getCalendar().getTeam();
        event.validateIsTeamEvent();

        validateTeamMember(team, member);

        List<Long> calendarIdsToValidate = getAllRelatedCalendarIds(team);

        eventQueryService.checkEventUpdateOverlap(
                calendarIdsToValidate,
                requestDto.startTime(),
                requestDto.endTime(),
                event
        );

        eventRawService.updateEvent(event, EventModifyRequestDto.toDto(requestDto));

        return EventGetResponseDto.fromSingleEvent(event);
    }

    @Transactional
    public EventGetResponseDto modifyTeamRecurringEvent(String email, Long eventId, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event event = eventRawService.findEventById(eventId);

        event.validateIsTeamEvent();
        Team team = event.getCalendar().getTeam();
        validateTeamMember(team, member);

        List<Long> calendarIdsToValidate = getAllRelatedCalendarIds(team);
        eventQueryService.checkEventUpdateOverlap(
                calendarIdsToValidate,
                requestDto.startTime(),
                requestDto.endTime(),
                event
        );

        eventRawService.updateEvent(event, EventModifyRequestDto.toDto(requestDto));
        // 기존 예외 삭제
        eventOverrideRawService.deleteAllEventOverrideByEvent(event);

        return EventGetResponseDto.fromRecurringEvent(event);
    }

    @Transactional
    public EventGetResponseDto modifyTeamRecurringInstance(String email, Long eventId, RecurringInstanceModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event originalEvent = eventRawService.findEventById(eventId);
        originalEvent.validateIsTeamEvent();
        Team team = originalEvent.getCalendar().getTeam();
        validateTeamMember(team, member);

        Optional<EventOverride> eventOverrideOpt = eventOverrideRawService.findEventOverride(originalEvent, requestDto.originalStartTime());

        EventOverride eventOverride;
        if (eventOverrideOpt.isPresent()) {
            eventOverride = eventOverrideOpt.get();
            eventOverrideRawService.updateEventOverride(eventOverride, requestDto.toEventOverrideDto());
        }
        else {
            eventOverride = EventOverride.makeEventOverride(originalEvent, requestDto.toEventOverrideDto());
        }
        EventOverride savedException = eventOverrideRawService.saveEventOverride(eventOverride);

        return EventGetResponseDto.fromEventOverride(savedException, originalEvent);
    }

    @Transactional
    public void deleteTeamEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);
        Event event = eventRawService.findEventById(eventId);

        Team team = event.getCalendar().getTeam();
        event.validateIsTeamEvent();
        validateTeamMember(team, member);
        eventRawService.deleteEvent(event);
    }
    
    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getTodayTeamEvents(String email, Long teamId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        return getTeamEventsForPeriod(email, teamId, start, end);
    }
    
    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getUpcomingTeamEvents(String email, Long teamId) {
        LocalDateTime start = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(8).atStartOfDay();
        return getTeamEventsForPeriod(email, teamId, start, end);
    }

    @Transactional
    public void deleteTeamRecurringEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);
        Event event = eventRawService.findEventById(eventId);
        event.validateIsTeamEvent();
        Team team = event.getCalendar().getTeam();
        validateTeamMember(team, member);

        recurringEventRawService.deleteRecurringEvent(event);
    }

    @Transactional
    public void deleteTeamRecurringInstance(String email, Long eventId, RecurringInstanceDeleteRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event originalEvent = eventRawService.findEventById(eventId);
        originalEvent.validateIsTeamEvent();
        Team team = originalEvent.getCalendar().getTeam();

        validateTeamMember(team, member);

        EventOverride eventOverride = EventOverride.makeEventDeleteOverride(originalEvent, requestDto.originalStartTime());
        eventOverrideRawService.saveEventOverride(eventOverride);
    }

    private void validateTeamMember(Team team, Member member) {
        teamMemberRawService.checkTeamAndMember(team, member);
    }


    /**
     * 현재 팀에 속해있는 모든 멤버에 대해 해당 멤버가 속한 모든 캘린더 id 조회
     * @param currentTeam
     * @return
     */
    private List<Long> getAllRelatedCalendarIds(Team currentTeam) {
        List<Member> teamMembers = getAllTeamMember(currentTeam);
        Set<Long> calendarIds = new HashSet<>();

        for (Member member : teamMembers) {
            calendarIds.add(calendarRawService.getMyPersonalCalendar(member).getCalendarId());

            List<TeamMember> memberships = teamMemberRawService.findByMember(member);
            for (TeamMember membership : memberships) {
                Team team = membership.getTeam();
                calendarIds.add(calendarRawService.getTeamCalendar(team).getCalendarId());
            }
        }
        return new ArrayList<>(calendarIds);
    }
    
    private List<EventGetResponseDto> getTeamEventsForPeriod(String email, Long teamId, LocalDateTime start, LocalDateTime end) {
        Member member = memberRawService.findMemberByEmail(email);
        Team team = teamRawService.findTeamById(teamId);
        
        validateTeamMember(team, member);
        
        Long calendarId = calendarRawService.getTeamCalendar(team).getCalendarId();
        List<EventServiceDto> events = eventQueryService.getEvents(List.of(calendarId), start, end);
        
        return events.stream()
            .map(EventGetResponseDto::fromServiceDto)
            .toList();
    }
    
    private List<Member> getAllTeamMember(Team team) {
        return teamMemberRawService.findByTeam(team)
                .stream()
                .map(TeamMember::getMember)
                .toList();
    }
}
