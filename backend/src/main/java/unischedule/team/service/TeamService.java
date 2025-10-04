package unischedule.team.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.common.dto.PaginationMetadataDto;
import unischedule.common.dto.PaginationRequestDto;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.TeamRole;
import unischedule.team.dto.*;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.service.internal.TeamCodeGenerator;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.team.service.internal.TeamRawService;

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

    @Transactional
    public void closeTeam(String email, Long teamId) {
        Team findTeam = teamRawService.findTeamById(teamId);

        Member findMember = memberRawService.findMemberByEmail(email);

        TeamMember findRelation = teamMemberRawService.findByTeamAndMember(findTeam, findMember);

        findRelation.checkLeader();
        
        /*
        팀 삭제 전 해야할 일이 생긴다면 여기 추가
         */

        List<TeamMember> findTeamMember = teamMemberRawService.findByTeam(findTeam);
        teamMemberRawService.deleteTeamMemberAll(findTeamMember);

        teamRawService.deleteTeam(findTeam);
    }

    /**
     * 팀 리스트 조회 메서드
     *
     * @param email          유저 이메일
     * @param paginationMeta 페이지네이션 정보
     * @return 팀 리스트와 페이지네이션 메타데이터를 담은 Dto
     */
    public TeamListResponseDto findAllTeams(String email, PaginationRequestDto paginationMeta) {
        Member findMember = memberRawService.findMemberByEmail(email);
        Page<Team> findTeams = teamRawService.findTeamByMemberWithNullableKeyword(findMember, paginationMeta);
        List<TeamResponseDto> teamResponseDtos = toTeamResponseDtos(findTeams.getContent());
        PaginationMetadataDto metadataDto = PaginationMetadataDto.from(findTeams);

        return new TeamListResponseDto(teamResponseDtos, metadataDto);
    }

    /**
     * 팀 리스트를 팀 응답 Dto 리스트로 변환
     *
     * @param teams 팀 리스트
     * @return 팀 응답 Dto 리스트
     */
    private List<TeamResponseDto> toTeamResponseDtos(List<Team> teams) {
        return teams.stream()
                .map(t -> {
                    List<MemberNameResponseDto> nameResponseDtos = toMemberNameResponseDtos(teamMemberRawService.findByTeam(t));
                    return new TeamResponseDto(
                            t.getTeamId(),
                            t.getName(),
                            nameResponseDtos,
                            nameResponseDtos.size(),
                            t.getInviteCode()
                    );
                })
                .toList();
    }

    /**
     * 팀 멤버 리스트를 멤버 이름 응답 Dto 리스트로 변환
     *
     * @param teamMembers 팀 멤버 리스트
     * @return 멤버 이름 응답 Dto 리스트
     */
    private List<MemberNameResponseDto> toMemberNameResponseDtos(List<TeamMember> teamMembers) {
        return teamMembers.stream()
                .map(MemberNameResponseDto::from)
                .toList();
    }
}
