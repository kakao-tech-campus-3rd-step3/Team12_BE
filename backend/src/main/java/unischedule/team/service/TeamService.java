package unischedule.team.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.repository.CalendarRepository;
import unischedule.exception.EntityNotFoundException;
import unischedule.member.entity.Member;
import unischedule.member.repository.MemberRepository;
import unischedule.team.dto.TeamCreateRequestDto;
import unischedule.team.dto.TeamCreateResponseDto;
import unischedule.team.dto.TeamJoinRequestDto;
import unischedule.team.dto.TeamJoinResponseDto;
import unischedule.team.entity.Team;
import unischedule.team.entity.TeamMember;
import unischedule.team.repository.TeamMemberRepository;
import unischedule.team.repository.TeamRepository;

/**
 * 팀과 관련한 서비스 메서드들의 클래스
 */
@Service
@RequiredArgsConstructor
public class TeamService {
    
    private final TeamRepository teamRepository;
    private final CalendarRepository calendarRepository;
    private final MemberRepository memberRepository;
    private final TeamMemberRepository teamMemberRepository;
    private static final TeamCodeGenerator TEAM_CODE_GENERATOR = new TeamCodeGenerator();
    
    /**
     * 팀 생성을 위한 메서드
     * @param email 헤더에서 얻어온 유저 이메일
     * @param requestDto 생성 요청 Dto, 팀 이름에 대한 정보 포함
     * @return 팀 생성된 결과를 담은 Dto
     */
    @Transactional
    public TeamCreateResponseDto createTeam(String email, TeamCreateRequestDto requestDto) {
        
        String code;
        
        Member findMember = memberRepository.findByEmail(email).get();
        
        do {
            code = TEAM_CODE_GENERATOR.generate();
        } while (teamRepository.existsByInviteCode(code));
        
        Team newTeam = new Team(requestDto.name(), code);
        
        Team saved = teamRepository.save(newTeam);
        
        TeamMember relation = new TeamMember(saved, findMember, "LEADER");
        teamMemberRepository.save(relation);
        
        Calendar teamCalendar = new Calendar(
            findMember, saved, "팀 캘린더", "팀 캘린더입니다."
        );
        
        calendarRepository.save(teamCalendar);
        
        return new TeamCreateResponseDto(
            saved.getTeamId(),
            saved.getName(),
            saved.getInviteCode()
        );
    }
    
    /**
     *
     * @param email 헤더에서 넘어온 유저 이메일
     * @param requestDto 팀 코드 요청
     * @return 가입된 팀의 정보
     */
    public TeamJoinResponseDto joinTeam(String email, TeamJoinRequestDto requestDto) {
        
        Team findTeam = teamRepository.findByInviteCode(requestDto.visitCode())
            .orElseThrow(() -> new EntityNotFoundException("요청한 정보의 팀이 없습니다."));
        
        Member findMember = memberRepository.findByEmail(email).get();
        
        TeamMember relation = new TeamMember(findTeam, findMember, "MEMBER");
        teamMemberRepository.save(relation);
        
        return new TeamJoinResponseDto(
            findTeam.getTeamId(),
            findTeam.getName(),
            findTeam.getInviteCode()
        );
    }
    
    @Transactional
    public void withdrawTeam(String email, Long teamId) {
        Team findTeam = teamRepository.findById(teamId)
            .orElseThrow(() -> new EntityNotFoundException("요청한 정보의 팀이 없습니다."));
        
        Member findMember = memberRepository.findByEmail(email).get();
        
        TeamMember findRelation = teamMemberRepository.findByTeamAndMember(findTeam, findMember)
            .orElseThrow(() -> new EntityNotFoundException("해당 팀 소속이 아닙니다."));
        
        /*
        팀 탈퇴 전 해야할 일이 생긴다면 여기 추가
         */
        
        teamMemberRepository.delete(findRelation);
    }
    
    public void closeTeam(String email, Long teamId) {
        Team findTeam = teamRepository.findById(teamId)
            .orElseThrow(() -> new EntityNotFoundException("요청한 정보의 팀이 없습니다."));
        
        Member findMember = memberRepository.findByEmail(email).get();
        
        TeamMember findRelation = teamMemberRepository.findByTeamAndMember(findTeam, findMember)
            .orElseThrow(() -> new EntityNotFoundException("해당 팀 소속이 아닙니다."));
        
        findRelation.checkLeader();
        
        /*
        팀 삭제 전 해야할 일이 생긴다면 여기 추가
         */
        
        List<TeamMember> findTeamMember = teamMemberRepository.findByTeam(findTeam);
        teamMemberRepository.deleteAll(findTeamMember);
        
        teamRepository.delete(findTeam);
    }
}
