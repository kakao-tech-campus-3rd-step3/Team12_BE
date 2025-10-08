package unischedule.team.service.internal;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.domain.Event;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.service.internal.EventRawService;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;

@Service
@RequiredArgsConstructor
public class WhenToMeetRawService {
    
    private final TeamRawService teamRawService;
    private final TeamMemberRawService teamMemberRawService;
    private final MemberRawService memberRawService;
    private final CalendarRawService calendarRawService;
    private final EventRawService eventRawService;
    
    @Transactional(readOnly = true)
    public List<Member> findTeamMembers(Long teamId) {
        Team team = teamRawService.findTeamById(teamId);
        return teamMemberRawService.findByTeam(team).stream()
            .map(TeamMember::getMember)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<EventGetResponseDto> findMemberEvents(Member member, LocalDateTime start, LocalDateTime end) {
        
        // 멤버의 이메일로 전체 팀 캘린더 및 개인 캘린더 식별
        Member findMember = memberRawService.findMemberByEmail(member.getEmail());
        
        List<Team> teamList = teamMemberRawService.findByMember(findMember)
            .stream()
            .map(TeamMember::getTeam)
            .toList();
        
        List<Long> calendarIds = new ArrayList<>();
        
        // 개인 캘린더 추가
        calendarIds.add(calendarRawService.getMyPersonalCalendar(findMember).getCalendarId());
        
        // 팀 캘린더 추가
        List<Long> teamCalendarIds = teamList.stream()
            .map(calendarRawService::getTeamCalendar)
            .map(Calendar::getCalendarId)
            .toList();
        
        calendarIds.addAll(teamCalendarIds);
        
        // 해당 기간의 모든 일정 조회
        List<Event> events = eventRawService.findSchedule(calendarIds, start, end);
        
        return events.stream()
            .map(EventGetResponseDto::from)
            .toList();
    }
}

