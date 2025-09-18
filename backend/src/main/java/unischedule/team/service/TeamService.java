package unischedule.team.service;

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
import unischedule.team.repository.TeamRepository;

/**
 * 팀과 관련한 서비스 메서드들의 클래스
 */
// 차후 개인과 팀의 연관관계 구현 후 완성 필요 (세환 씨 저랑 이야기 좀 해요)
@Service
@RequiredArgsConstructor
public class TeamService {
    
    private final TeamRepository teamRepository;
    private final CalendarRepository calendarRepository;
    private final MemberRepository memberRepository;
    
    /**
     * 팀 생성을 위한 메서드
     * @param email 헤더에서 얻어온 유저 이메일
     * @param requestDto 생성 요청 Dto, 팀 이름에 대한 정보 포함
     * @return 팀 생성된 결과를 담은 Dto
     */
    @Transactional
    public TeamCreateResponseDto createTeam(String email, TeamCreateRequestDto requestDto) {
        
        TeamCodeGenerator teamCodeGenerator = new TeamCodeGenerator();
        String code;
        
        Member findMember = memberRepository.findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException("해당 멤버가 없습니다."));
        
        do {
            code = teamCodeGenerator.generate();
        } while (teamRepository.existsByInviteCode(code));
        
        Team newTeam = new Team(requestDto.name(), code);
        
        Team saved = teamRepository.save(newTeam);
        
        /*
        // 팀과 개인의 연관관계 구현해서 저장하는 코드 추가 필요 (중계 테이블 필요 때문에 반드시 소통 필요)
         */
        
        Calendar teamCalendar = new Calendar(
            findMember, saved, "팀 캘린더", "팀 캘린더입니다."
        ); //유저 정보 불러오기 필요
        
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
        
        /*
        팀과 개인의 연관관계 구현해서 저장하는 코드 추가 필요
         */
        
        return new TeamJoinResponseDto(
            findTeam.getTeamId(),
            findTeam.getName(),
            findTeam.getInviteCode()
        );
    }
}
