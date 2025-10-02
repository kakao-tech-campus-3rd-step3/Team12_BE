package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.TeamEventCreateRequestDto;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventState;
import unischedule.events.service.internal.EventRawService;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.NoPermissionException;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.repository.TeamMemberRepository;
import unischedule.team.repository.TeamRepository;
import unischedule.team.service.internal.TeamRawService;

@Service
@RequiredArgsConstructor
public class TeamEventService {
    private final MemberRawService memberRawService;
    private final EventRawService eventRawService;
    private final CalendarRawService calendarRawService;
    private final TeamRawService teamRawService;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public EventCreateResponseDto createTeamEvent(String email, TeamEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);

        Team team = teamRawService.findTeamById(requestDto.teamId());

        validateTeamMember(team, member);

        Calendar calendar = calendarRawService.getTeamCalendar(team);

        Event event = new Event(
                requestDto.title(),
                requestDto.description(),
                requestDto.startTime(),
                requestDto.endTime(),
                EventState.PENDING,
                requestDto.isPrivate()
        );

        event.connectCalendar(calendar);

        return EventCreateResponseDto.from(eventRawService.saveEvent(event));
    }

    @Transactional
    public EventGetResponseDto modifyTeamEvent(String email, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);

        Event event = eventRawService.findEventById(requestDto.eventId());

        Team team = event.getCalendar().getTeam();
        event.validateIsTeamEvent();

        validateTeamMember(team, member);

        // 팀 일정 수정 가능여부 체크 필요

        event.modifyEvent(requestDto);

        return EventGetResponseDto.from(event);
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
        teamMemberRepository.findByTeamAndMember(team, member)
                .orElseThrow(() -> new NoPermissionException("팀에 속해 있지 않습니다."));
    }
}
