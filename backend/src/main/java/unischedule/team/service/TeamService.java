package unischedule.team.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.common.dto.PageResponseDto;
import unischedule.common.dto.PaginationRequestDto;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.domain.TeamRole;
import unischedule.team.dto.*;
import unischedule.team.domain.WhenToMeet;
import unischedule.team.service.internal.TeamCodeGenerator;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.team.service.internal.TeamRawService;
import unischedule.team.service.internal.WhenToMeetLogicService;
import unischedule.team.service.internal.WhenToMeetRawService;

/**
 * 팀과 관련한 서비스 메서드들의 클래스
 */
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRawService teamRawService;
    private final CalendarRawService calendarRawService;
    private final MemberRawService memberRawService;
    private final TeamMemberRawService teamMemberRawService;
    private final WhenToMeetRawService whenToMeetRawService;
    private final WhenToMeetLogicService whenToMeetLogicService;
    private final TeamCodeGenerator teamCodeGenerator = new TeamCodeGenerator();

    /**
     * 팀 생성을 위한 메서드
     *
     * @param email      헤더에서 얻어온 유저 이메일
     * @param requestDto 생성 요청 Dto, 팀 이름에 대한 정보 포함
     * @return 팀 생성된 결과를 담은 Dto
     */
    @Transactional
    public TeamCreateResponseDto createTeam(String email, TeamCreateRequestDto requestDto) {

        Member findMember = memberRawService.findMemberByEmail(email);

        String code = generateTeamCode();

        Team newTeam = new Team(requestDto.teamName(), requestDto.teamDescription(), code);

        Team saved = teamRawService.saveTeam(newTeam);

        TeamMember relation = new TeamMember(saved, findMember, TeamRole.LEADER);
        teamMemberRawService.saveTeamMember(relation);

        Calendar teamCalendar = new Calendar(findMember, saved);

        calendarRawService.saveCalendar(teamCalendar);

        return new TeamCreateResponseDto(
                saved.getTeamId(),
                saved.getName(),
                saved.getDescription(),
                saved.getInviteCode()
        );
    }

    private String generateTeamCode() {
        String code;
        do {
            code = teamCodeGenerator.generate();
        } while (teamRawService.existsByInviteCode(code));
        return code;
    }

    /**
     *
     * @param email      헤더에서 넘어온 유저 이메일
     * @param requestDto 팀 코드 요청
     * @return 가입된 팀의 정보
     */
    public TeamJoinResponseDto joinTeam(String email, TeamJoinRequestDto requestDto) {

        Team findTeam = teamRawService.findTeamByInviteCode(requestDto.inviteCode());

        Member findMember = memberRawService.findMemberByEmail(email);

        teamMemberRawService.validateDuplication(findTeam, findMember);

        TeamMember relation = new TeamMember(findTeam, findMember, TeamRole.MEMBER);
        teamMemberRawService.saveTeamMember(relation);

        return new TeamJoinResponseDto(
                findTeam.getTeamId(),
                findTeam.getName(),
                findTeam.getDescription()
        );
    }
    
    /**
     * 팀 탈퇴
     *
     * @param email  이메일
     * @param teamId 팀 아이디
     */
    @Transactional
    public void withdrawTeam(String email, Long teamId) {
        Team findTeam = teamRawService.findTeamById(teamId);

        Member findMember = memberRawService.findMemberByEmail(email);

        TeamMember findRelation = teamMemberRawService.findByTeamAndMember(findTeam, findMember);
        
        /*
        팀 탈퇴 전 해야할 일이 생긴다면 여기 추가
         */

        teamMemberRawService.deleteTeamMember(findRelation);
    }
    
    /**
     * 팀 삭제
     * @param email 이메일
     * @param teamId 팀 아이디
     */
    @Transactional
    public void closeTeam(String email, Long teamId) {
        Team findTeam = teamRawService.findTeamById(teamId);

        Member findMember = memberRawService.findMemberByEmail(email);

        Calendar findCalendar = calendarRawService.getTeamCalendar(findTeam);

        TeamMember findRelation = teamMemberRawService.findByTeamAndMember(findTeam, findMember);

        findRelation.checkLeader();
        
        /*
        팀 삭제 전 해야할 일이 생긴다면 여기 추가
         */

        List<TeamMember> findTeamMember = teamMemberRawService.findByTeam(findTeam);
        teamMemberRawService.deleteTeamMemberAll(findTeamMember);
        
        calendarRawService.deleteCalendar(findCalendar);

        teamRawService.deleteTeam(findTeam);
    }
    
    /**
     * 일정 겹치는 것 체크
     *
     * @param teamId
     * @return 겹치는 일정 리스트
     */
    //현재 "겹치는 일정"이 없다고 보고 만든 코드
    public List<WhenToMeetResponseDto> getTeamMembersWhenToMeet(Long teamId) {
        List<Member> members = whenToMeetRawService.findTeamMembers(teamId);
        List<LocalDateTime> starts = whenToMeetLogicService.generateIntervalStarts();
        List<LocalDateTime> ends = whenToMeetLogicService.generateIntervalEnds();

        List<WhenToMeet> slots = whenToMeetLogicService.generateSlots(members, starts, ends);
        whenToMeetLogicService.applyMemberEvents(slots, members, starts, ends, whenToMeetRawService);

        return whenToMeetLogicService.toResponse(slots);
    }

    /**
     * 사용자가 속한 팀들을 페이징 처리하여 조회하는 메서드
     *
     * @param email          헤더에서 넘어온 유저 이메일
     * @param paginationMeta 페이징 및 검색 정보
     * @return 사용자가 속한 팀들의 페이징된 결과
     */
    @Transactional(readOnly = true)
    public PageResponseDto<TeamResponseDto> findMyTeamsWithMembers(String email, PaginationRequestDto paginationMeta) {
        Member findMember = memberRawService.findMemberByEmail(email);
        Page<Team> findTeams = teamRawService.findTeamsByMember(findMember, paginationMeta);
        Page<TeamResponseDto> responseDtos = findTeams.map(team -> {
            List<MemberNameResponseDto> memberDtos = toMemberNameResponseDtos(teamMemberRawService.findByTeam(team));
            return new TeamResponseDto(
                    team.getTeamId(),
                    team.getName(),
                    memberDtos,
                    memberDtos.size(),
                    team.getInviteCode()
            );
        });

        return PageResponseDto.from(responseDtos);
    }

    /**
     * TeamMember 리스트를 MemberNameResponseDto 리스트로 변환하는 메서드
     *
     * @param teamMembers 팀 멤버 엔티티 리스트
     * @return 멤버 이름 응답 Dto 리스트
     */
    private List<MemberNameResponseDto> toMemberNameResponseDtos(List<TeamMember> teamMembers) {

        return teamMembers.stream()
                .map(MemberNameResponseDto::from)
                .toList();
    }

    /**
     * 팀에서 멤버를 제거하는 메서드
     *
     * @param requestDto 요청 Dto, 리더 이메일, 팀 아이디, 제거할 멤버 아이디 포함
     */
    @Transactional
    public void removeMemberFromTeam(RemoveMemberCommandDto requestDto) {
        Team findTeam = teamRawService.findTeamById(requestDto.teamId());
        Member leaderMember = memberRawService.findMemberByEmail(requestDto.leaderEmail());
        Member targetMember = memberRawService.findMemberById(requestDto.targetMemberId());
        TeamMember leader = teamMemberRawService.findByTeamAndMember(findTeam, leaderMember);
        TeamMember target = teamMemberRawService.findByTeamAndMember(findTeam, targetMember);
        leader.checkLeader();
        target.validateRemovable();

        teamMemberRawService.deleteTeamMember(target);
    }
}
