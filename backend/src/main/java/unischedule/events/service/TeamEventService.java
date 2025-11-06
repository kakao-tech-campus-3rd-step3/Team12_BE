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
import unischedule.events.dto.TeamEventGetResponseDto;
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
import java.util.Comparator;
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

        Event savedEvent = eventCommandService.createSingleEvent(calendar, requestDto.toDto());

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

        Event savedEvent = eventCommandService.createRecurringEvent(calendar, requestDto);

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
    public List<TeamEventGetResponseDto> getTeamEvents(String email, Long teamId, LocalDateTime startAt, LocalDateTime endAt) {
        return getTeamAllEvent(email, teamId, startAt, endAt);
    }

    private List<TeamEventGetResponseDto> getTeamAllEvent(String email, Long teamId, LocalDateTime startAt, LocalDateTime endAt) {
        Member member = memberRawService.findMemberByEmail(email);
        Team team = teamRawService.findTeamById(teamId);

        validateTeamMember(team, member);

        Calendar teamCalendar = calendarRawService.getTeamCalendar(team);

        List<EventServiceDto> serviceDtos = eventQueryService.getEvents(List.of(teamCalendar.getCalendarId()), startAt, endAt);

        return serviceDtos.stream()
                .map(dto -> {
                    Event event = eventRawService.findEventById(dto.eventId());
                    List<Long> participantIds = getParticipantIdsForEvent(event);

                    return TeamEventGetResponseDto.from(dto, participantIds);
                })
                .toList();
    }

    @Transactional
    public EventGetResponseDto modifyTeamEvent(String email, Long eventId, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event event = eventRawService.findEventById(eventId);
        Team team = event.getCalendar().getTeam();
        event.validateIsTeamEvent();
        validateTeamMember(team, member);

        Event modifiedEvent = eventCommandService.modifySingleEvent(event, requestDto.toDto());

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

        Event modifiedEvent = eventCommandService.modifyRecurringEvent(
                event,
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
    public List<TeamEventGetResponseDto> getTodayTeamEvents(String email, Long teamId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        List<TeamEventGetResponseDto> allEvents = getTeamAllEvent(email, teamId, start, end);
        
        return allEvents.stream()
            .sorted(Comparator.comparing(TeamEventGetResponseDto::startTime))
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<TeamEventGetResponseDto> getUpcomingTeamEvents(String email, Long teamId) {
        LocalDateTime start = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(8).atStartOfDay();
        return getTeamAllEvent(email, teamId, start, end)
            .stream()
            .sorted(Comparator.comparing(TeamEventGetResponseDto::startTime))
            .toList();
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

    private List<Long> getParticipantIdsForEvent(Event event) {
        if (event.isForAllMembers()) {
            Team team = event.getCalendar().getTeam();
            if (team == null) {
                return List.of();
            }

            return teamMemberRawService.findByTeam(team)
                    .stream()
                    .map(TeamMember::getMember)
                    .map(Member::getMemberId)
                    .toList();
        }

        return eventParticipantRawService.getParticipantsForEvent(event)
                .stream()
                .map(Member::getMemberId)
                .toList();
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
    
    private List<Member> getAllTeamMember(Team team) {
        return teamMemberRawService.findByTeam(team)
                .stream()
                .map(TeamMember::getMember)
                .toList();
    }
}
