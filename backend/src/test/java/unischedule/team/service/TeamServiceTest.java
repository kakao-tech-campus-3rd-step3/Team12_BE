package unischedule.team.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.service.PersonalEventService;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.domain.TeamRole;
import unischedule.team.dto.TeamCreateRequestDto;
import unischedule.team.dto.TeamJoinRequestDto;
import unischedule.team.dto.WhenToMeetResponseDto;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.team.service.internal.TeamRawService;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {
    @Mock
    private TeamRawService teamRawService;
    @Mock
    private CalendarRawService calendarRawService;
    @Mock
    private MemberRawService memberRawService;
    @Mock
    private TeamMemberRawService teamMemberRawService;
    @Mock
    private PersonalEventService personalEventService;
    
    @InjectMocks
    private TeamService teamService;
    
    
    @Test
    @DisplayName("팀 생성 시 팀, 팀멤버, 캘린더가 생성된다")
    void createTeam_success() {
        // given
        String email = "test@test.com";
        Member member = new Member(email, "test", "1q2w3e4r!");
        TeamCreateRequestDto requestDto = new TeamCreateRequestDto("TeamA", "설명입니다.");
        
        when(memberRawService.findMemberByEmail(email)).thenReturn(member);
        when(teamRawService.existsByInviteCode(anyString())).thenReturn(false);
        
        Team mockTeam = new Team("TeamA", "설명입니다.", "ABCDE");
        when(teamRawService.saveTeam(any(Team.class))).thenReturn(mockTeam);
        
        // when
        var result = teamService.createTeam(email, requestDto);
        
        // then
        verify(teamRawService, times(1)).saveTeam(any(Team.class));
        verify(teamMemberRawService, times(1)).saveTeamMember(any(TeamMember.class));
        verify(calendarRawService, times(1)).saveCalendar(any(Calendar.class));
        
        assertThat(result.teamName()).isEqualTo("TeamA");
        assertThat(result.teamCode()).isNotNull();
    }
    
    @Test
    @DisplayName("팀 코드로 팀 가입 성공 시 팀멤버 생성")
    void joinTeam_success() {
        // given
        String email = "test@test.com";
        TeamJoinRequestDto requestDto = new TeamJoinRequestDto("CODE123");
        
        Team team = new Team("TeamA", "팀 설명", "CODE123");
        Member member = new Member(email, "test", "1q2w3e4r!");
        
        when(teamRawService.findTeamByInviteCode("CODE123")).thenReturn(team);
        when(memberRawService.findMemberByEmail(email)).thenReturn(member);
        
        // when
        var result = teamService.joinTeam(email, requestDto);
        
        // then
        verify(teamMemberRawService, times(1)).saveTeamMember(any(TeamMember.class));
        assertThat(result.teamName()).isEqualTo("TeamA");
    }
    
    @Test
    @DisplayName("팀 탈퇴 시 팀멤버가 삭제된다")
    void withdrawTeam_success() {
        // given
        String email = "test@test.com";
        Long teamId = 1L;
        
        Team team = new Team("TeamA", "설명", "CODE123");
        Member member = new Member(email, "test", "1q2w3e4r!");
        TeamMember teamMember = new TeamMember(team, member, TeamRole.MEMBER);
        
        when(teamRawService.findTeamById(teamId)).thenReturn(team);
        when(memberRawService.findMemberByEmail(email)).thenReturn(member);
        when(teamMemberRawService.findByTeamAndMember(team, member)).thenReturn(teamMember);
        
        // when
        teamService.withdrawTeam(email, teamId);
        
        // then
        verify(teamMemberRawService, times(1)).deleteTeamMember(teamMember);
    }
    
    @Test
    @DisplayName("모든 팀원이 일정이 없으면 availableMember == 팀원 수 유지")
    void getTeamMembersWhenToMeet_noEvents() {
        // given
        Long teamId = 1L;
        Team team = new Team("TeamA", "설명", "CODE123");
        Member member1 = new Member("email@email.com", "test", "1q2w3e4r!");
        Member member2 = new Member("email2@email.com", "test", "1q2w3e4r!");
        
        when(teamRawService.findTeamById(teamId)).thenReturn(team);
        when(teamMemberRawService.findByTeam(team))
            .thenReturn(List.of(new TeamMember(team, member1, TeamRole.MEMBER),
                new TeamMember(team, member2, TeamRole.MEMBER)));
        
        when(personalEventService.getPersonalEvents(anyString(), any(), any()))
            .thenReturn(Collections.emptyList());
        
        // when
        List<WhenToMeetResponseDto> result = teamService.getTeamMembersWhenToMeet(teamId);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.get(0).getAvailableMember()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("팀원이 일정이 있으면 해당 슬롯의 availableMember 감소")
    void getTeamMembersWhenToMeet_withOverlap() {
        // given
        Long teamId = 1L;
        Team team = new Team("TeamA", "설명", "CODE123");
        Member member = new Member("email@email.com", "test", "1q2w3e4r!");
        
        when(teamRawService.findTeamById(teamId)).thenReturn(team);
        when(teamMemberRawService.findByTeam(team))
            .thenReturn(List.of(new TeamMember(team, member, TeamRole.MEMBER)));
        
        // 특정 시간대에 이벤트가 존재한다고 가정
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9);
        LocalDateTime end = start.plusHours(1);
        
        when(personalEventService.getPersonalEvents(anyString(), any(), any()))
            .thenReturn(List.of(new EventGetResponseDto(1L, "회의", "", start, end, false)));
        
        // when
        List<WhenToMeetResponseDto> result = teamService.getTeamMembersWhenToMeet(teamId);
        
        // then
        long affectedSlots = result.stream()
            .filter(s -> s.getAvailableMember() < 1)
            .count();
        
        assertThat(affectedSlots).isGreaterThan(0);
    }
}