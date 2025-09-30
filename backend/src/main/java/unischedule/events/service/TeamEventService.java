package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarDomainService;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.TeamEventCreateRequestDto;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventState;
import unischedule.events.service.internal.EventDomainService;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.NoPermissionException;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberDomainService;
import unischedule.team.domain.Team;
import unischedule.team.repository.TeamMemberRepository;
import unischedule.team.repository.TeamRepository;

@Service
@RequiredArgsConstructor
public class TeamEventService {
    private final MemberDomainService memberDomainService;
    private final EventDomainService eventDomainService;
    private final CalendarDomainService calendarDomainService;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public EventCreateResponseDto createTeamEvent(String email, TeamEventCreateRequestDto requestDto) {
        Member member = memberDomainService.findMemberByEmail(email);

        Team team = findTeamById(requestDto.teamId());

        validateTeamMember(team, member);

        Calendar calendar = calendarDomainService.getTeamCalendar(team);

        Event event = new Event(
                requestDto.title(),
                requestDto.description(),
                requestDto.startTime(),
                requestDto.endTime(),
                EventState.PENDING,
                requestDto.isPrivate()
        );

        event.connectCalendar(calendar);

        return EventCreateResponseDto.from(eventDomainService.saveEvent(event));
    }

    /*
    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getTeamEvents(String email, Long teamId, LocalDateTime startAt, LocalDateTime endAt) {
        Member member = memberDomainService.findMemberByEmail(email);

        Team team = findTeamById(teamId);

        Calendar calendar = calendarDomainService.getTeamCalendar(team);

        return List.of(null);
    }

     */

    private Team findTeamById(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("팀을 찾을 수 없습니다."));
    }

    private void validateTeamMember(Team team, Member member) {
        teamMemberRepository.findByTeamAndMember(team, member)
                .orElseThrow(() -> new NoPermissionException("팀에 속해 있지 않습니다."));
    }
}
