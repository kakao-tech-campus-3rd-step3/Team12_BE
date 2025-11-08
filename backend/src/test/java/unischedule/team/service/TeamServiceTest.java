package unischedule.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.common.dto.PageResponseDto;
import unischedule.common.dto.PaginationRequestDto;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.NoPermissionException;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.domain.TeamRole;
import unischedule.team.domain.WhenToMeet;
import unischedule.team.dto.TeamCreateRequestDto;
import unischedule.team.dto.TeamDetailResponseDto;
import unischedule.team.dto.TeamJoinRequestDto;
import unischedule.team.dto.TeamResponseDto;
import unischedule.team.dto.WhenToMeetRecommendResponseDto;
import unischedule.team.dto.WhenToMeetResponseDto;
import unischedule.team.dto.RemoveMemberCommandDto;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.team.service.internal.TeamRawService;
import unischedule.team.service.internal.WhenToMeetLogicService;
import unischedule.team.service.internal.WhenToMeetRawService;
import unischedule.util.TestUtil;


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
    private WhenToMeetRawService whenToMeetRawService;
    @Mock
    private WhenToMeetLogicService whenToMeetLogicService;

    @InjectMocks
    private TeamService teamService;

    @DisplayName("사용자의 팀 목록을 페이지 형태로 조회할 수 있다")
    @Test
    void findMyTeamsWithMembers_shouldReturnPagedTeams() {

        Member testMember = new Member("test@email.com", "tester", "password123");

        Team testTeam = new Team("테스트팀", "Test Description", "abc123");
        TeamMember testTeamMember = new TeamMember(testTeam, testMember, TeamRole.MEMBER);
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
        Member member1 = new Member("email@email.com", "test", "1q2w3e4r!");
        Member member2 = new Member("email2@email.com", "test2", "1q2w3e4r!");
        List<Member> members = List.of(member1, member2);

        // 1시간짜리 간단한 슬롯만 가정
        LocalDateTime start = LocalDateTime.of(2025, 10, 10, 9, 0);
        LocalDateTime end = start.plusHours(1);
        List<LocalDateTime> starts = List.of(start);
        List<LocalDateTime> ends = List.of(end);
        List<WhenToMeet> slots = List.of(new WhenToMeet(start, end, (long) members.size()));

        // mock 설정
        when(whenToMeetRawService.findTeamMembers(teamId)).thenReturn(members);
        when(whenToMeetLogicService.generateIntervalStarts()).thenReturn(starts);
        when(whenToMeetLogicService.generateIntervalEnds()).thenReturn(ends);
        when(whenToMeetLogicService.generateSlots(members, starts, ends)).thenReturn(slots);

        // 일정이 없으므로 applyMemberEvents는 아무 일도 하지 않음
        doNothing().when(whenToMeetLogicService)
                .applyMemberEvents(slots, members, starts, ends, whenToMeetRawService);

        // 반환 DTO mock
        when(whenToMeetLogicService.toResponse(slots))
                .thenReturn(List.of(new WhenToMeetResponseDto(start, end, (long) members.size())));

        // when
        List<WhenToMeetResponseDto> result = teamService.getTeamMembersWhenToMeet(teamId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).availableMember()).isEqualTo(2); // 팀원 2명 그대로 유지
    }

    @Test
    @DisplayName("팀원이 일정이 있으면 해당 슬롯의 availableMember 감소")
    void getTeamMembersWhenToMeet_withOverlap() {
        // given
        Long teamId = 1L;
        Member member = new Member("email@email.com", "test", "1q2w3e4r!");
        List<Member> members = List.of(member);

        List<LocalDateTime> starts = List.of(LocalDateTime.of(2025, 10, 10, 9, 0));
        List<LocalDateTime> ends = List.of(starts.get(0).plusHours(1));

        List<WhenToMeet> slots = List.of(new WhenToMeet(starts.get(0), ends.get(0), 1L)); // availableMember=1

        // mock
        when(whenToMeetRawService.findTeamMembers(teamId)).thenReturn(members);
        when(whenToMeetLogicService.generateIntervalStarts()).thenReturn(starts);
        when(whenToMeetLogicService.generateIntervalEnds()).thenReturn(ends);
        when(whenToMeetLogicService.generateSlots(members, starts, ends)).thenReturn(slots);

        // applyMemberEvents() 내부에서 RawService를 사용하지만, 외부에서 검증할 건 없음
        doAnswer(invocation -> {
            List<WhenToMeet> slotList = invocation.getArgument(0);
            slotList.get(0).discountAvailable(); // 실제 로직 흉내
            return null;
        }).when(whenToMeetLogicService).applyMemberEvents(any(), any(), any(), any(), any());

        when(whenToMeetLogicService.toResponse(slots))
                .thenReturn(List.of(new WhenToMeetResponseDto(starts.get(0), ends.get(0), 0L)));

        // when
        List<WhenToMeetResponseDto> result = teamService.getTeamMembersWhenToMeet(teamId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).availableMember()).isEqualTo(0);
    }

    @DisplayName("팀장이 팀원을 정상적으로 제거할 수 있다.")
    @Test
    void 팀장이_팀원을_제거한다() {
        // given
        Team team = new Team("TeamA", "설명", "CODE123");
        Member leader = new Member("leader@email.com", "nickname1", "1234");
        Member member = new Member("member@email.com", "nickname2", "1234");

        TeamMember leaderTeamMember = new TeamMember(team, leader, TeamRole.LEADER);
        TeamMember memberTeamMember = new TeamMember(team, member, TeamRole.MEMBER);

        RemoveMemberCommandDto requestDto = new RemoveMemberCommandDto(
                leader.getEmail(),
                team.getTeamId(),
                member.getMemberId()
        );

        when(teamRawService.findTeamById(requestDto.teamId())).thenReturn(team);
        when(memberRawService.findMemberByEmail(requestDto.leaderEmail())).thenReturn(leader);
        when(memberRawService.findMemberById(requestDto.targetMemberId())).thenReturn(member);
        when(teamMemberRawService.findByTeamAndMember(team, leader)).thenReturn(leaderTeamMember);
        when(teamMemberRawService.findByTeamAndMember(team, member)).thenReturn(memberTeamMember);

        // when
        teamService.removeMemberFromTeam(requestDto);

        // then
        verify(teamMemberRawService, times(1)).deleteTeamMember(memberTeamMember);
    }

    @DisplayName("팀장을 제거하려 하면 예외 발생")
    @Test
    void 팀장을_팀에서_제거한다() {
        // given
        Team team = new Team("테스트팀", "설명", "CODE123");
        Member leader = new Member("leader@email.com", "team leader", "1234");
        Member targetLeader = new Member("target@email.com", "target user", "5678");

        TeamMember leaderTeamMember = new TeamMember(team, leader, TeamRole.LEADER);
        TeamMember targetTeamMember = new TeamMember(team, targetLeader, TeamRole.LEADER);

        RemoveMemberCommandDto requestDto = new RemoveMemberCommandDto(
                leader.getEmail(),
                team.getTeamId(),
                targetLeader.getMemberId()
        );

        when(teamRawService.findTeamById(requestDto.teamId())).thenReturn(team);
        when(memberRawService.findMemberByEmail(requestDto.leaderEmail())).thenReturn(leader);
        when(memberRawService.findMemberById(requestDto.targetMemberId())).thenReturn(targetLeader);
        when(teamMemberRawService.findByTeamAndMember(team, leader)).thenReturn(leaderTeamMember);
        when(teamMemberRawService.findByTeamAndMember(team, targetLeader)).thenReturn(targetTeamMember);

        // expect
        assertThatThrownBy(() -> teamService.removeMemberFromTeam(requestDto))
                .isInstanceOf(NoPermissionException.class)
                .hasMessage("팀장 권한을 가진 멤버는 제거할 수 없습니다.");

        verify(teamMemberRawService, never()).deleteTeamMember(any());
    }

    @DisplayName("리더가 아닌 멤버가 팀원을 제거할 경우 오류발생")
    @Test
    void 리더가_아닌_멤버가_팀원을_제거할_경우_오류발생() {
        // given
        Team team = new Team("TeamA", "설명", "CODE123");
        Member member1 = new Member("member1@email.com", "nickname1", "1234");
        Member member2 = new Member("member2@email.com", "nickname2", "1234");

        TeamMember teamMember1 = new TeamMember(team, member1, TeamRole.MEMBER);
        TeamMember teamMember2 = new TeamMember(team, member2, TeamRole.MEMBER);

        RemoveMemberCommandDto requestDto = new RemoveMemberCommandDto(
                member1.getEmail(),
                team.getTeamId(),
                member2.getMemberId()
        );

        when(teamRawService.findTeamById(requestDto.teamId())).thenReturn(team);
        when(memberRawService.findMemberByEmail(requestDto.leaderEmail())).thenReturn(member1);
        when(memberRawService.findMemberById(requestDto.targetMemberId())).thenReturn(member2);
        when(teamMemberRawService.findByTeamAndMember(team, member1)).thenReturn(teamMember1);
        when(teamMemberRawService.findByTeamAndMember(team, member2)).thenReturn(teamMember2);

        // when & then
        assertThatThrownBy(() -> teamService.removeMemberFromTeam(requestDto))
                .isInstanceOf(NoPermissionException.class)
                .hasMessage("리더가 아닙니다.");

        verify(teamMemberRawService, never()).deleteTeamMember(any());
    }
    
    @Test
    @DisplayName("팀 일정 추천 시, 모든 멤버가 가능한 시간을 1순위로 반환한다")
    void getOptimalTimeWhenToMeet_Success() {
        
        //GIVEN
        Long teamId = 1L;
        LocalDateTime startTime = LocalDateTime.of(2025, 11, 1, 9, 0); // 11/1 (토) 09:00
        LocalDateTime endTime = LocalDateTime.of(2025, 11, 1, 12, 0); // 11/1 (토) 12:00
        Long requiredTime = 60L; // 60분 (4슬롯)
        Long requiredCnt = 1L;   // 1개 추천
        
        Member member1 = TestUtil.makeMember();
        Member member2 = TestUtil.makeMember();
        List<Member> members = List.of(member1, member2);
        Long totalMembers = (long) members.size(); // 2L
        
        when(whenToMeetRawService.findTeamMembers(teamId)).thenReturn(members);
        
        List<LocalDateTime> intervalStarts = List.of(startTime);
        List<LocalDateTime> intervalEnds = List.of(endTime);
        when(whenToMeetLogicService.generateIntervalStarts(startTime, endTime)).thenReturn(intervalStarts);
        when(whenToMeetLogicService.generateIntervalEnds(startTime, endTime)).thenReturn(intervalEnds);
        
        List<WhenToMeet> initialSlots = TestUtil.createInitialSlots(startTime, endTime, members.size());
        when(whenToMeetLogicService.generateSlots(members, intervalStarts, intervalEnds)).thenReturn(initialSlots);

        doAnswer(invocation -> {
            List<WhenToMeet> slotsToModify = invocation.getArgument(0);
            slotsToModify.get(4).discountAvailable(); // 10:00-10:15
            slotsToModify.get(5).discountAvailable(); // 10:15-10:30
            slotsToModify.get(6).discountAvailable(); // 10:30-10:45
            slotsToModify.get(7).discountAvailable(); // 10:45-11:00
            return null; // void 메서드이므로 null 반환
        }).when(whenToMeetLogicService).applyMemberEvents(
            eq(initialSlots), eq(members), eq(intervalStarts), eq(intervalEnds), eq(whenToMeetRawService)
        );
        
        WhenToMeetRecommendResponseDto expectedRecommendation = new WhenToMeetRecommendResponseDto(
            "토",
            "최적",
            LocalDateTime.of(2025, 11, 1, 9, 0),  // startTime
            LocalDateTime.of(2025, 11, 1, 10, 0), // endTime (requiredTime 60분 뒤)
            totalMembers
        );
        List<WhenToMeetRecommendResponseDto> expectedResultList = List.of(expectedRecommendation);
        
        when(whenToMeetLogicService.recommendBestSlots(
            eq(initialSlots), // applyMemberEvents에 의해 수정된 바로 그 리스트
            eq(requiredTime), // 60L
            eq(requiredCnt),  // 1L
            eq(totalMembers)  // 2L
        )).thenReturn(expectedResultList);
        
        
        // WHEN
        List<WhenToMeetRecommendResponseDto> result = teamService.getOptimalTimeWhenToMeet(
            startTime, endTime, requiredTime, requiredCnt, teamId
        );
        
        // THEN
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1); // requiredCnt = 1
        
        WhenToMeetRecommendResponseDto topPick = result.get(0);
        assertThat(topPick.available()).isEqualTo(2L);
        assertThat(topPick.status()).isEqualTo("최적");
        assertThat(topPick.week()).isEqualTo("토");
        assertThat(topPick.startTime()).isEqualTo(LocalDateTime.of(2025, 11, 1, 9, 0));
        assertThat(topPick.endTime()).isEqualTo(LocalDateTime.of(2025, 11, 1, 10, 0));
        
        // VERIFY
        // 모든 의존성 메서드들이 올바른 인자들로 호출되었는지 검증합니다.
        verify(whenToMeetRawService).findTeamMembers(teamId);
        verify(whenToMeetLogicService).generateIntervalStarts(startTime, endTime);
        verify(whenToMeetLogicService).generateIntervalEnds(startTime, endTime);
        verify(whenToMeetLogicService).generateSlots(members, intervalStarts, intervalEnds);
        verify(whenToMeetLogicService).applyMemberEvents(initialSlots, members, intervalStarts, intervalEnds, whenToMeetRawService);
        
        // recommendBestSlots가 마지막에 올바른 인자들로 호출되었는지 검증합니다.
        verify(whenToMeetLogicService).recommendBestSlots(
            initialSlots, requiredTime, requiredCnt, totalMembers
        );
    }
    
    @Test
    @DisplayName("팀 일정 추천 시, 최적(2명) 2개, 보통(1명) 1개를 순서대로 반환한다")
    void getOptimalTimeWhenToMeet_Success_Top3() {
        // GIVEN
        Long teamId = 1L;
        LocalDateTime startTime = LocalDateTime.of(2025, 10, 30, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 10, 30, 12, 0);
        Long requiredTime = 60L; // 60분 (4슬롯)
        Long requiredCnt = 3L;   // 3개 추천
        
        Member member1 = TestUtil.makeMember();
        Member member2 = TestUtil.makeMember();
        List<Member> members = List.of(member1, member2);
        long totalMembers = members.size(); // 2L
        when(whenToMeetRawService.findTeamMembers(teamId)).thenReturn(members);
        
        List<LocalDateTime> intervalStarts = List.of(startTime);
        List<LocalDateTime> intervalEnds = List.of(endTime);
        when(whenToMeetLogicService.generateIntervalStarts(startTime, endTime)).thenReturn(intervalStarts);
        when(whenToMeetLogicService.generateIntervalEnds(startTime, endTime)).thenReturn(intervalEnds);
        
        List<WhenToMeet> initialSlots = TestUtil.createInitialSlots(startTime, endTime, totalMembers);
        when(whenToMeetLogicService.generateSlots(members, intervalStarts, intervalEnds)).thenReturn(initialSlots);
        
        // applyMemberEvents가 initialSlots를 수정하는 것을 시뮬레이션
        doAnswer(invocation -> {
            List<WhenToMeet> slotsToModify = invocation.getArgument(0);
            slotsToModify.get(4).discountAvailable(); // 10:00-10:15 (available=1)
            slotsToModify.get(5).discountAvailable(); // 10:15-10:30 (available=1)
            slotsToModify.get(6).discountAvailable(); // 10:30-10:45 (available=1)
            slotsToModify.get(7).discountAvailable(); // 10:45-11:00 (available=1)
            return null; // void 메서드
        }).when(whenToMeetLogicService).applyMemberEvents(
            eq(initialSlots), eq(members), eq(intervalStarts), eq(intervalEnds), eq(whenToMeetRawService)
        );
        
        WhenToMeetRecommendResponseDto expectedTop1 = new WhenToMeetRecommendResponseDto(
            "목",    // week
            "최적",   // status
            LocalDateTime.of(2025, 10, 30, 9, 0),  // startTime
            LocalDateTime.of(2025, 10, 30, 10, 0), // endTime
            2L       // available
        );
        
        WhenToMeetRecommendResponseDto expectedTop2 = new WhenToMeetRecommendResponseDto(
            "목",    // week
            "최적",   // status
            LocalDateTime.of(2025, 10, 30, 11, 0), // startTime
            LocalDateTime.of(2025, 10, 30, 12, 0), // endTime
            2L       // available
        );
        
        WhenToMeetRecommendResponseDto expectedTop3 = new WhenToMeetRecommendResponseDto(
            "목",    // week
            "보통",   // status
            LocalDateTime.of(2025, 10, 30, 9, 15), // startTime
            LocalDateTime.of(2025, 10, 30, 10, 15),// endTime
            1L       // available
        );
        List<WhenToMeetRecommendResponseDto> expectedResultList = List.of(expectedTop1, expectedTop2, expectedTop3);
        
        when(whenToMeetLogicService.recommendBestSlots(
            eq(initialSlots), // applyMemberEvents에 의해 수정된 바로 그 리스트
            eq(requiredTime), // 60L
            eq(requiredCnt),  // 3L
            eq(totalMembers)  // 2L
        )).thenReturn(expectedResultList);
        
        
        // WHEN
        List<WhenToMeetRecommendResponseDto> result = teamService.getOptimalTimeWhenToMeet(
            startTime, endTime, requiredTime, requiredCnt, teamId
        );
        
        //THEN
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3); // 3개 반환 확인 (GIVEN에서 설정한대로)
        
        // 1순위 검증
        WhenToMeetRecommendResponseDto top1 = result.get(0);
        assertThat(top1.available()).isEqualTo(2L);
        assertThat(top1.status()).isEqualTo("최적");
        assertThat(top1.week()).isEqualTo("목");
        assertThat(top1.startTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 9, 0));
        assertThat(top1.endTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 10, 0));
        
        // 2순위 검증
        WhenToMeetRecommendResponseDto top2 = result.get(1);
        assertThat(top2.available()).isEqualTo(2L);
        assertThat(top2.status()).isEqualTo("최적");
        assertThat(top2.week()).isEqualTo("목");
        assertThat(top2.startTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 11, 0));
        assertThat(top2.endTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 12, 0));
        
        // 3순위 검증
        WhenToMeetRecommendResponseDto top3 = result.get(2);
        assertThat(top3.available()).isEqualTo(1L);
        assertThat(top3.status()).isEqualTo("보통");
        assertThat(top3.week()).isEqualTo("목");
        assertThat(top3.startTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 9, 15));
        assertThat(top3.endTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 10, 15));
        
        //VERIFY
        // 기존 verify 호출 확인
        verify(whenToMeetRawService).findTeamMembers(teamId);
        verify(whenToMeetLogicService).generateSlots(any(), any(), any());
        verify(whenToMeetLogicService).applyMemberEvents(any(), any(), any(), any(), any());
        verify(whenToMeetLogicService).recommendBestSlots(
            eq(initialSlots), eq(requiredTime), eq(requiredCnt), eq(totalMembers)
        );
    }
  
    @Test
    @DisplayName("해당 소속 팀의 멤버가 아닐 경우 예외 발생")
    void 해당_소속팀이_아닐_경우_예외_발생() {
        //given
        Member member1 = new Member("member1@email.com", "nickname1", "1234");
        Team team = new Team("TeamA", "설명", "CODE123");
        PaginationRequestDto paginationMeta = new PaginationRequestDto(1, 10, null);

        when(memberRawService.findMemberByEmail(anyString())).thenReturn(member1);
        when(teamRawService.findTeamById(any())).thenReturn(team);
        doThrow(EntityNotFoundException.class)
                .when(teamMemberRawService)
                .validateMembership(team, member1);

        //when & then
        assertThatThrownBy(() -> teamService.getTeamMembers(member1.getEmail(), team.getTeamId(), paginationMeta))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("팀 상세 정보를 정상적으로 조회한다")
    void 팀_상세_정보를_정상적으로_조회한다() {
        // given
        String email = "test@kakao.com";
        Long teamId = 1L;

        Team team = new Team("카테캠 FE", "카카오 테크 캠퍼스 3단계 프로젝트", "WHFFU5");
        Member member = new Member(email, "홍길동", "securePassword");
        int memberCount = 5;
        when(teamRawService.findTeamById(teamId)).thenReturn(team);
        when(memberRawService.findMemberByEmail(email)).thenReturn(member);
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);
        when(teamMemberRawService.countTeamMemberByTeam(team)).thenReturn(memberCount);

        // when
        TeamDetailResponseDto response = teamService.getTeamDetail(email, teamId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("카테캠 FE");
        assertThat(response.description()).isEqualTo("카카오 테크 캠퍼스 3단계 프로젝트");
        assertThat(response.count()).isEqualTo(5);
        assertThat(response.code()).isEqualTo("WHFFU5");

        // verify (Mock 호출 검증)
        verify(teamRawService).findTeamById(teamId);
        verify(memberRawService).findMemberByEmail(email);
        verify(teamMemberRawService).checkTeamAndMember(team, member);
        verify(teamMemberRawService).countTeamMemberByTeam(team);
    }

    @Test
    @DisplayName("팀 상세 정보 조회 시, 팀에 속하지 않은 멤버는 예외가 발생한다")
    void 팀_상세_정보_조회_시_팀에_속하지_않은_멤버는_예외가_발생한다() {
        // given
        String email = "test@kakao.com";
        Long teamId = 1L;
        Team team = new Team("카테캠 FE", "카카오 테크 캠퍼스 3단계 프로젝트", "WHFFU5");
        Member member = new Member(email, "홍길동", "securePassword");

        when(teamRawService.findTeamById(teamId)).thenReturn(team);
        when(memberRawService.findMemberByEmail(email)).thenReturn(member);
        doThrow(EntityNotFoundException.class)
                .when(teamMemberRawService)
                .checkTeamAndMember(team, member);

        //when & then
        assertThatThrownBy(() -> teamService.getTeamDetail(email, teamId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
