package unischedule.team.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import unischedule.common.dto.PaginationRequestDto;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.domain.TeamRole;
import unischedule.team.dto.TeamListResponseDto;
import unischedule.team.dto.TeamResponseDto;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.team.service.internal.TeamRawService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TeamServiceTest {

    @Mock
    private MemberRawService memberRawService;

    @Mock
    private TeamRawService teamRawService;

    @Mock
    private TeamMemberRawService teamMemberRawService;

    @InjectMocks
    private TeamService teamService;

    @Test
    @DisplayName("사용자의 이메일로 속한 팀 목록과 각 팀의 멤버를 조회할 수 있다")
    void 사용자의_이메일로_속한_팀목록과_각팀의_멤버를_조회할_수_있다() {
        // given
        String email = "test@email.com";
        PaginationRequestDto pagination = new PaginationRequestDto(1, 10, null);

        Member mockMember = new Member("test@email.com", "user1", "encodedPw");

        Team team1 = new Team("Backend", "백엔드 스터디", "INVITE123");
        Team team2 = new Team("Frontend", "프론트 스터디", "INVITE456");
        List<Team> mockTeams = List.of(team1, team2);
        Page<Team> teamPage = new PageImpl<>(mockTeams, PageRequest.of(0, 10), mockTeams.size());

        TeamMember tm1 = new TeamMember(team1, mockMember, TeamRole.LEADER);
        TeamMember tm2 = new TeamMember(team2, mockMember, TeamRole.MEMBER);

        when(memberRawService.findMemberByEmail(email)).thenReturn(mockMember);
        when(teamRawService.findTeamByMemberWithNullableKeyword(mockMember, pagination)).thenReturn(teamPage);
        when(teamMemberRawService.findByTeam(team1)).thenReturn(List.of(tm1));
        when(teamMemberRawService.findByTeam(team2)).thenReturn(List.of(tm2));

        // when
        TeamListResponseDto response = teamService.findAllTeams(email, pagination);

        // then
        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(2);

        TeamResponseDto firstTeam = response.items().get(0);
        assertThat(firstTeam.teamName()).isEqualTo("Backend");
        assertThat(firstTeam.members()).hasSize(1);

        assertThat(response.pagination().page()).isEqualTo(1);
        assertThat(response.pagination().totalCount()).isEqualTo(2);

        verify(memberRawService).findMemberByEmail(email);
        verify(teamRawService).findTeamByMemberWithNullableKeyword(mockMember, pagination);
    }
}
