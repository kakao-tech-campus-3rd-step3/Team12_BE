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
import unischedule.events.dto.RecurringEventCreateRequestDto;
import unischedule.events.dto.RecurringInstanceDeleteRequestDto;
import unischedule.events.dto.RecurringInstanceModifyRequestDto;
import unischedule.events.dto.TeamEventCreateRequestDto;
import unischedule.events.service.common.EventCommandService;
import unischedule.events.service.common.EventQueryService;
import unischedule.events.service.internal.EventParticipantRawService;
import unischedule.events.service.internal.EventRawService;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeamEventService {
    private final MemberRawService memberRawService;
    private final EventRawService eventRawService;
    private final CalendarRawService calendarRawService;
    private final TeamRawService teamRawService;
    private final TeamMemberRawService teamMemberRawService;
    private final EventQueryService eventQueryService;
    private final EventCommandService eventCommandService;
    private final EventParticipantRawService eventParticipantRawService;

    @Transactional
    public EventCreateResponseDto createTeamSingleEvent(String email, TeamEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Team team = teamRawService.findTeamById(requestDto.teamId());
        validateTeamMember(team, member);
        Calendar calendar = calendarRawService.getTeamCalendar(team);

        List<Member> participants = getParticipants(team, requestDto.eventParticipants());

        List<Long> calendarIdsToValidate = getCalendarIdsForMembers(participants);

        Event savedEvent = eventCommandService.createSingleEvent(calendar, calendarIdsToValidate, requestDto.toDto());

        handleEventParticipants(savedEvent, participants, requestDto.eventParticipants());

        return EventCreateResponseDto.from(savedEvent);
    }

    @Transactional
    public EventCreateResponseDto createTeamRecurringEvent(String email, Long teamId, RecurringEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Team team = teamRawService.findTeamById(teamId);
        validateTeamMember(team, member);
        Calendar calendar = calendarRawService.getTeamCalendar(team);

        List<Member> participants = getParticipants(team, requestDto.eventParticipants());
        List<Long> calendarIdsToValidate = getCalendarIdsForMembers(participants);

        Event savedEvent = eventCommandService.createRecurringEvent(calendar, calendarIdsToValidate, requestDto);

        handleEventParticipants(savedEvent, participants, requestDto.eventParticipants());

        return EventCreateResponseDto.from(savedEvent);
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

        List<Long> calendarIdsToValidate = getCalendarIdsForModification(event, team, requestDto);

        Event modifiedEvent = eventCommandService.modifySingleEvent(event, calendarIdsToValidate, requestDto.toDto());

        if (requestDto.eventParticipants() != null) {
            List<Member> participants = getParticipants(team, requestDto.eventParticipants());
            handleEventParticipants(modifiedEvent, participants, requestDto.eventParticipants());
        }

        return EventGetResponseDto.fromSingleEvent(event);
    }

    @Transactional
    public EventGetResponseDto modifyTeamRecurringEvent(String email, Long eventId, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event event = eventRawService.findEventById(eventId);

        event.validateIsTeamEvent();
        Team team = event.getCalendar().getTeam();
        validateTeamMember(team, member);

        List<Long> calendarIdsToValidate = getCalendarIdsForModification(event, team, requestDto);

        Event modifiedEvent = eventCommandService.modifyRecurringEvent(
                event,
                calendarIdsToValidate,
                requestDto.toDto()
        );

        if (requestDto.eventParticipants() != null) {
            List<Member> participants = getParticipants(team, requestDto.eventParticipants());
            handleEventParticipants(modifiedEvent, participants, requestDto.eventParticipants());
        }

        return EventGetResponseDto.fromRecurringEvent(event);
    }

    @Transactional
    public EventGetResponseDto modifyTeamRecurringInstance(String email, Long eventId, RecurringInstanceModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event originalEvent = eventRawService.findEventById(eventId);
        originalEvent.validateIsTeamEvent();
        Team team = originalEvent.getCalendar().getTeam();
        validateTeamMember(team, member);

        EventOverride eventOverride = eventCommandService.modifyRecurringInstance(originalEvent, requestDto);

        return EventGetResponseDto.fromEventOverride(eventOverride, originalEvent);
    }

    @Transactional
    public void deleteTeamEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);
        Event event = eventRawService.findEventById(eventId);

        Team team = event.getCalendar().getTeam();
        event.validateIsTeamEvent();
        validateTeamMember(team, member);
        eventCommandService.deleteSingleEvent(event);
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

        eventCommandService.deleteRecurringEvent(event);
    }

    @Transactional
    public void deleteTeamRecurringInstance(String email, Long eventId, RecurringInstanceDeleteRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event originalEvent = eventRawService.findEventById(eventId);
        originalEvent.validateIsTeamEvent();
        Team team = originalEvent.getCalendar().getTeam();

        validateTeamMember(team, member);

        eventCommandService.deleteRecurringEventInstance(originalEvent, requestDto);
    }

    private void validateTeamMember(Team team, Member member) {
        teamMemberRawService.checkTeamAndMember(team, member);
    }

    private List<Long> getCalendarIdsForMembers(List<Member> members) {
        Set<Long> calendarIds = new HashSet<>();
        for (Member member : members) {

            calendarIds.add(calendarRawService.getMyPersonalCalendar(member).getCalendarId());

            List<TeamMember> memberships = teamMemberRawService.findByMember(member);
            for (TeamMember membership : memberships) {
                Team team = membership.getTeam();
                calendarIds.add(calendarRawService.getTeamCalendar(team).getCalendarId());
            }
        }
        return new ArrayList<>(calendarIds);
    }

    /**
     * 팀 이벤트 참여자 검증 및 반환
     * @param team
     * @param participantMemberIds
     * @return
     */
    private List<Member> getParticipants(Team team, List<Long> participantMemberIds) {
        if (participantMemberIds == null || participantMemberIds.isEmpty()) {
            return getAllTeamMember(team);
        }

        Set<Long> memberIdSet = new HashSet<>(participantMemberIds);
        List<Member> participants = new ArrayList<>();

        for (Long memberid : memberIdSet) {
            Member member = memberRawService.findMemberById(memberid);

            teamMemberRawService.checkTeamAndMember(team, member);
            participants.add(member);
        }
        return participants;
    }

    /**
     * 수정 시 일정 충돌 검사가 필요한 캘린더 목록 반환
     * @param event
     * @param team
     * @param requestDto
     * @return
     */
    private List<Long> getCalendarIdsForModification(Event event, Team team, EventModifyRequestDto requestDto) {
        boolean timeChanged = (requestDto.startTime() != null || requestDto.endTime() != null);

        // 참여자 변경 없을 시
        if (requestDto.eventParticipants() == null) {
            if (!timeChanged) {
                return List.of();
            }

            List<Member> currentParticipants = getCurrentParticipants(event, team);
            return getCalendarIdsForMembers(currentParticipants);
        }

        // 참여자 변경 시
        List<Member> newParticipants = getParticipants(team, requestDto.eventParticipants());
        return getCalendarIdsForMembers(newParticipants);
    }

    private List<Member> getCurrentParticipants(Event event, Team team) {
        if (event.isForAllMembers()) {
            return getAllTeamMember(team);
        }
        else {
            return eventParticipantRawService.getParticipantsForEvent(event);
        }
    }

    private void handleEventParticipants(Event event, List<Member> allParticipants, List<Long> newParticipantIds) {
        if (newParticipantIds == null) {
            return;
        }

        eventParticipantRawService.deleteAllParticipantsByEvent(event);

        if (newParticipantIds.isEmpty()) {
            event.updateIsSelective(false);
        }
        else {
            event.updateIsSelective(true);

            eventParticipantRawService.saveAllParticipantsForEvent(event, allParticipants);
        }
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
