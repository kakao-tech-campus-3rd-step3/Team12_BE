package unischedule.team.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import unischedule.common.dto.PageResponseDto;
import unischedule.common.dto.PaginationRequestDto;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.domain.TeamRole;
import unischedule.team.dto.TeamResponseDto;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.team.service.internal.TeamRawService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TeamServiceTest {

    @Mock
    private MemberRawService memberRawService;

    @Mock
    private TeamRawService teamRawService;

    @Mock
    private TeamMemberRawService teamMemberRawService;

    @InjectMocks
    private TeamService teamService;

    private Member testMember;
    private Team testTeam;
    private TeamMember testTeamMember;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testMember = new Member("test@email.com", "tester", "password123");
        testTeam = new Team("테스트팀", "Test Description", "abc123");
        testTeamMember = new TeamMember(testTeam, testMember, TeamRole.MEMBER);
    }

    @DisplayName("사용자의 팀 목록을 페이지 형태로 조회할 수 있다")
    @Test
    void findMyTeamsWithMembers_shouldReturnPagedTeams() {
        // given
        PaginationRequestDto paginationMeta = new PaginationRequestDto(1, 10, null);

        // Mock: 이메일로 Member 조회
        when(memberRawService.findMemberByEmail("test@email.com"))
                .thenReturn(testMember);

        // Mock: 사용자가 속한 Team 페이지 조회
        Page<Team> mockPage = new PageImpl<>(List.of(testTeam), PageRequest.of(0, 10), 1);
        when(teamRawService.findTeamsByMember(any(Member.class), any(PaginationRequestDto.class)))
                .thenReturn(mockPage);

        // Mock: Team별 멤버 조회
        when(teamMemberRawService.findByTeam(any(Team.class)))
                .thenReturn(List.of(testTeamMember));

        // when
        PageResponseDto<TeamResponseDto> result =
                teamService.findMyTeamsWithMembers("test@email.com", paginationMeta);

        // then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);

        TeamResponseDto teamDto = result.content().get(0);
        assertThat(teamDto.teamName()).isEqualTo("테스트팀");
        assertThat(teamDto.memberCount()).isEqualTo(1);
        assertThat(teamDto.members().get(0).name()).isEqualTo("tester");

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }
}
