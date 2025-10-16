package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventState;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.TeamEventCreateRequestDto;
import unischedule.events.service.internal.EventRawService;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.team.service.internal.TeamRawService;

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

    @Transactional
    public EventCreateResponseDto createTeamEvent(String email, TeamEventCreateRequestDto requestDto) {
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

    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getTeamEvents(String email, Long teamId, LocalDateTime startAt, LocalDateTime endAt) {
        Member member = memberRawService.findMemberByEmail(email);
        Team team = teamRawService.findTeamById(teamId);

        validateTeamMember(team, member);

        Calendar teamCalendar = calendarRawService.getTeamCalendar(team);

        List<Event> events = eventRawService.findSchedule(List.of(teamCalendar.getCalendarId()), startAt, endAt);

        return events.stream()
                .map(EventGetResponseDto::fromSingleEvent)
                .toList();
    }


    @Transactional
    public EventGetResponseDto modifyTeamEvent(String email, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);

        Event event = eventRawService.findEventById(requestDto.eventId());

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
    public void deleteTeamEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);
        Event event = eventRawService.findEventById(eventId);

        Team team = event.getCalendar().getTeam();
        event.validateIsTeamEvent();
        validateTeamMember(team, member);
        eventRawService.deleteEvent(event);
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
    private List<Member> getAllTeamMember(Team team) {
        return teamMemberRawService.findByTeam(team)
                .stream()
                .map(TeamMember::getMember)
                .toList();
    }
}
