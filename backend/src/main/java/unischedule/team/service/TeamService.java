package unischedule.team.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.service.PersonalEventService;
import unischedule.events.service.internal.EventRawService;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.TeamRole;
import unischedule.team.dto.TeamCreateRequestDto;
import unischedule.team.dto.TeamCreateResponseDto;
import unischedule.team.dto.TeamJoinRequestDto;
import unischedule.team.dto.TeamJoinResponseDto;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.dto.WhenToMeetResponseDto;
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
    private final PersonalEventService personalEventService;
    private final TeamCodeGenerator teamCodeGenerator = new TeamCodeGenerator();
    
    /**
     * 팀 생성을 위한 메서드
     * @param email 헤더에서 얻어온 유저 이메일
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
     * @param email 헤더에서 넘어온 유저 이메일
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
     * @param email 이메일
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
    
    
    //현재 "겹치는 일정"이 없다고 보고 만든 코드
    public List<WhenToMeetResponseDto> getTeamMembersWhenToMeet(Long teamId) {
        //돌려줄 결과
        List<WhenToMeetResponseDto> result = new ArrayList<>();
        
        //시작일과 끝일의 목록 저장
        List<LocalDateTime> intervalStarts =
            IntStream.rangeClosed(1, 7)
                .mapToObj(i -> LocalDate.now().plusDays(i).atTime(9, 0))
                .toList();
        
        List<LocalDateTime> intervalEnds =
            IntStream.rangeClosed(1, 7)
                .mapToObj(i -> LocalDate.now().plusDays(i + 1).atStartOfDay())
                .toList();
        
        //멤버 조회
        Team findTeam = teamRawService.findTeamById(teamId);
        
        List<Member> findMembers = teamMemberRawService.findByTeam(findTeam).stream()
            .map(TeamMember::getMember)
            .toList();
        
        //15분 간격 슬롯 생성
        for (int i = 0; i < intervalStarts.size(); i++) {
            LocalDateTime start = intervalStarts.get(i);
            LocalDateTime end = intervalEnds.get(i);
            
            LocalDateTime cursor = start;
            while (cursor.isBefore(end)) {
                LocalDateTime slotStart = cursor;
                LocalDateTime slotEnd = cursor.plusMinutes(15);
                
                if (slotEnd.isAfter(end)) {
                    slotEnd = end;
                }
                
                result.add(new WhenToMeetResponseDto(slotStart, slotEnd, (long) findMembers.size()));
                
                cursor = slotEnd;
            }
        }
        
        //멤버들의 일정을 전부 돌면서 슬롯과 겹치는지 확인
        for (Member member : findMembers) {
            for(int i = 0; i < intervalStarts.size(); i++) {
                List<EventGetResponseDto> personalEvents = personalEventService.getPersonalEvents(member.getEmail(), intervalStarts.get(i), intervalEnds.get(i));
                
                // 각 이벤트에 대해 15분 슬롯과 겹치는지 체크
                for (EventGetResponseDto event : personalEvents) {
                    LocalDateTime eventStart = event.startTime();
                    LocalDateTime eventEnd = event.endTime();
                    
                    // result의 모든 슬롯을 돌면서 겹침 확인
                    for (WhenToMeetResponseDto slot : result) {
                        // 슬롯이 이 날짜 범위에 속하지 않으면 건너뜀
                        if (slot.getStartTime().isBefore(intervalStarts.get(i)) || slot.getEndTime().isAfter(intervalEnds.get(i))) {
                            continue;
                        }
                        
                        // 이벤트와 슬롯이 겹치면 availableMember 감소
                        if (slot.getStartTime().isBefore(eventEnd) && slot.getEndTime().isAfter(eventStart)) {
                            slot.discountAvailable();
                        }
                    }
                }
            }
        }
        
        return result;
    }
}
